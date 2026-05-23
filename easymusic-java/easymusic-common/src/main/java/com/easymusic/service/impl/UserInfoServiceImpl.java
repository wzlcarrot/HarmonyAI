package com.easymusic.service.impl;

import java.util.Date;
import java.util.List;


import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.enums.UserStatusEnum;
import com.easymusic.exception.BusinessException;
import com.easymusic.redis.BloomFilterComponent;
import com.easymusic.redis.RedisComponent;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;

import com.easymusic.entity.enums.PageSize;
import com.easymusic.entity.query.UserInfoQuery;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.mappers.UserInfoMapper;
import com.easymusic.service.UserInfoService;
import com.easymusic.utils.StringTools;

import javax.annotation.Resource;


/**
 * 用户信息 业务接口实现
 */
@Service("userInfoService")
@RequiredArgsConstructor
@Slf4j
public class UserInfoServiceImpl implements UserInfoService {

	private final UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	private final RedisComponent redisComponent;

	private final BloomFilterComponent bloomFilterComponent;

	private final RedissonClient redissonClient;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 * 
	 * 使用两层缓存架构：Redis -> MySQL
	 * 还使用布隆过滤器防穿透
	 * 
	 * 流程：
	 * 1. 布隆过滤器判断：如果返回false，说明用户一定不存在，直接返回null
	 * 2. 查Redis缓存（由 redisComponent.getUserInfo 内部处理）：如果缓存命中，直接返回
	 * 3. 查数据库：如果缓存未命中，查询数据库
	 * 4. 更新缓存和布隆过滤器：无论数据是否存在，都更新布隆过滤器
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		// 1. 布隆过滤器判断
		if (!bloomFilterComponent.mightContainUser(userId)) {
			log.debug("布隆过滤器判断用户不存在, userId={}", userId);
			return null;
		}

		// 2. 查 Redis（由 redisComponent.getUserInfo 内部处理）
		UserInfo userInfo = redisComponent.getUserInfo(userId);
		if (userInfo != null) {
			log.debug("从缓存获取用户信息, userId={}", userId);
			return userInfo;
		}

		// 3. Redis 未命中时，使用分布式锁防止缓存击穿
		String lockKey = "lock:user:" + userId;
		RLock lock = redissonClient.getLock(lockKey);
		
		try {
			// 尝试获取锁，等待100ms，锁定3秒
			if (lock.tryLock(100, 3000, java.util.concurrent.TimeUnit.MILLISECONDS)) {
				try {
					// 双重检查：再次查询 Redis（可能其他线程已经加载了）
					userInfo = redisComponent.getUserInfo(userId);
					if (userInfo != null) {
						return userInfo;
					}
					
					// 查询数据库
					log.debug("缓存未命中，查询数据库, userId={}", userId);
					userInfo = this.userInfoMapper.selectByUserId(userId);
					
					if (userInfo != null) {
						// 存入 Redis（会自动使用随机过期时间）以及布隆过滤器
						redisComponent.saveUserInfo(userId, userInfo);
						bloomFilterComponent.addUser(userId);
						log.debug("用户信息查询成功并缓存, userId={}", userId);
					} else {
						// 即使不存在，也加入布隆过滤器，防止下次再查数据库
						bloomFilterComponent.addUser(userId);
						log.debug("用户不存在，已加入布隆过滤器, userId={}", userId);
					}
				} finally {
					// 释放锁
					if (lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				}
			} else {
				// 获取锁失败，等待一小段时间后重试 Redis
				Thread.sleep(50);
				userInfo = redisComponent.getUserInfo(userId);
				// 如果还是没拿到，降级：直接查询数据库
				if (userInfo == null) {
					userInfo = this.userInfoMapper.selectByUserId(userId);
					if (userInfo != null) {
						redisComponent.saveUserInfo(userId, userInfo);
						bloomFilterComponent.addUser(userId);
					} else {
						bloomFilterComponent.addUser(userId);
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("获取用户信息锁被中断, userId={}", userId, e);
			// 降级：直接查询数据库
			userInfo = this.userInfoMapper.selectByUserId(userId);
			if (userInfo != null) {
				redisComponent.saveUserInfo(userId, userInfo);
				bloomFilterComponent.addUser(userId);
			} else {
				bloomFilterComponent.addUser(userId);
			}
		}

		return userInfo;
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	@Override
	public void register(String email, String password, String nickName) {
		UserInfo userInfo = userInfoMapper.selectByEmail(email);

		if(userInfo!=null){
			throw new BusinessException("邮箱账号已经存在");
		}

		Date curDate = new Date();
		String userId = StringTools.getRandomNumber(Constants.LENGTH_12);
		userInfo = new UserInfo();
		userInfo.setUserId(userId);
		userInfo.setEmail(email);
		userInfo.setNickName(nickName);
		userInfo.setPassword(StringTools.encodeByMD5(password));
		userInfo.setCreateTime(curDate);
		userInfo.setLastLoginTime(curDate);
		userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
		userInfo.setAvatar(Constants.DEFAULT_AVATAR_PATH);

		userInfoMapper.insert(userInfo);

		// 注册成功后，将用户加入布隆过滤器并写入缓存，避免首次通过 userId 查询时被布隆过滤器误判为不存在
		bloomFilterComponent.addUser(userId);
		redisComponent.saveUserInfo(userId, userInfo);
	}

	@Override
	public TokenUserInfoDTO login(String email, String password) {

		UserInfo userInfo = userInfoMapper.selectByEmail(email);

		if(userInfo==null||userInfo.getPassword().equals(password)==false){
			throw new BusinessException("邮箱账号或者密码错误");
		}

		if(UserStatusEnum.DISABLE.getStatus()==userInfo.getStatus()){
			throw new BusinessException("账号被禁用");
		}

		UserInfo updateUserInfo = new UserInfo();
		updateUserInfo.setLastLoginTime(new Date());
		userInfoMapper.updateByUserId(updateUserInfo, userInfo.getUserId());

		TokenUserInfoDTO  tokenUserInfoDTO = new TokenUserInfoDTO();
		tokenUserInfoDTO.setUserId(userInfo.getUserId());
		tokenUserInfoDTO.setNickName(userInfo.getNickName());
		tokenUserInfoDTO.setAvatar(userInfo.getAvatar());
		tokenUserInfoDTO.setIntegral(userInfo.getIntegral());

		String token = tokenUserInfoDTO.getUserId()+StringTools.getRandomNumber(Constants.LENGTH_20);
		tokenUserInfoDTO.setToken(token);

		// 登录成功后，更新缓存（布隆过滤器在注册时已加入，无需重复添加）
		redisComponent.saveUserInfo(userInfo.getUserId(), userInfo);

		redisComponent.saveTokenUserInfoDTO(tokenUserInfoDTO);
		return tokenUserInfoDTO;
	}


}