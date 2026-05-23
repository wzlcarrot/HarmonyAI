package com.easymusic.service.impl;

import java.util.List;


import com.easymusic.entity.enums.MusicActionTypeEnum;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.query.UserInfoQuery;
import com.easymusic.mappers.MusicInfoMapper;
import com.easymusic.mappers.UserInfoMapper;
import com.easymusic.redis.RedisComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.easymusic.entity.enums.PageSize;
import com.easymusic.entity.query.MusicInfoActionQuery;
import com.easymusic.entity.po.MusicInfoAction;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.mappers.MusicInfoActionMapper;
import com.easymusic.service.MusicInfoActionService;
import com.easymusic.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


/**
 * 音乐操作 业务接口实现
 */
@Service("musicInfoActionService")
@Slf4j
@RequiredArgsConstructor
public class MusicInfoActionServiceImpl implements MusicInfoActionService {

	private final MusicInfoActionMapper<MusicInfoAction, MusicInfoActionQuery> musicInfoActionMapper;

	private final MusicInfoMapper<MusicInfo, MusicInfoQuery> musicInfoMapper;

	private final UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	private final RedisComponent redisComponent;
	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MusicInfoAction> findListByParam(MusicInfoActionQuery param) {
		return this.musicInfoActionMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MusicInfoActionQuery param) {
		return this.musicInfoActionMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MusicInfoAction> findListByPage(MusicInfoActionQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MusicInfoAction> list = this.findListByParam(param);
		PaginationResultVO<MusicInfoAction> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MusicInfoAction bean) {
		return this.musicInfoActionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MusicInfoAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.musicInfoActionMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MusicInfoAction> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.musicInfoActionMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MusicInfoAction bean, MusicInfoActionQuery param) {
		StringTools.checkParam(param);
		return this.musicInfoActionMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MusicInfoActionQuery param) {
		StringTools.checkParam(param);
		return this.musicInfoActionMapper.deleteByParam(param);
	}

	/**
	 * 根据ActionId获取对象
	 */
	@Override
	public MusicInfoAction getMusicInfoActionByActionId(Integer actionId) {
		return this.musicInfoActionMapper.selectByActionId(actionId);
	}

	/**
	 * 根据ActionId修改
	 */
	@Override
	public Integer updateMusicInfoActionByActionId(MusicInfoAction bean, Integer actionId) {
		return this.musicInfoActionMapper.updateByActionId(bean, actionId);
	}

	/**
	 * 根据ActionId删除
	 */
	@Override
	public Integer deleteMusicInfoActionByActionId(Integer actionId) {
		return this.musicInfoActionMapper.deleteByActionId(actionId);
	}

	/**
	 * 根据MusicIdAndUserId获取对象
	 */
	@Override
	public MusicInfoAction getMusicInfoActionByMusicIdAndUserId(String musicId, String userId) {
		return this.musicInfoActionMapper.selectByMusicIdAndUserId(musicId, userId);
	}

	/**
	 * 根据MusicIdAndUserId修改
	 */
	@Override
	public Integer updateMusicInfoActionByMusicIdAndUserId(MusicInfoAction bean, String musicId, String userId) {
		return this.musicInfoActionMapper.updateByMusicIdAndUserId(bean, musicId, userId);
	}

	/**
	 * 根据MusicIdAndUserId删除
	 */
	@Override
	public Integer deleteMusicInfoActionByMusicIdAndUserId(String musicId, String userId) {
		return this.musicInfoActionMapper.deleteByMusicIdAndUserId(musicId, userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void doGood(String musicId, String userId) {

		// 查询用户是否已经对这首音乐执行过操作（点赞）
		MusicInfoAction musicInfoAction = this.musicInfoActionMapper.selectByMusicIdAndUserId(musicId, userId);
		// 如果查询结果不为空，说明用户已经点过赞，执行取消点赞
		if (musicInfoAction != null) {
			// 删除点赞记录
			this.musicInfoActionMapper.deleteByMusicIdAndUserId(musicId, userId);
			// 更新音乐点赞数（减1）
			this.musicInfoMapper.updateGoodCount(musicId, -1);
			// 更新Redis缓存
			updateMusicInfoCache(musicId);
			return;
		}
		// 如果没有点赞记录，先检查音乐是否存在
		MusicInfo musicInfo = this.musicInfoMapper.selectByMusicId(musicId);
		if (musicInfo == null) {
			return;
		}
		// 插入点赞记录
		musicInfoAction = new MusicInfoAction();
		musicInfoAction.setMusicId(musicId);
		musicInfoAction.setMusicUserId(musicInfo.getUserId());
		musicInfoAction.setActionType(MusicActionTypeEnum.GOOD.getType());
		musicInfoAction.setUserId(userId);
		this.musicInfoActionMapper.insert(musicInfoAction);
		// 更新音乐点赞数（加1）
		this.musicInfoMapper.updateGoodCount(musicId, 1);
		// 更新Redis缓存
		updateMusicInfoCache(musicId);
	}

	/**
	 * 更新音乐信息缓存（点赞数变化后需要更新缓存）
	 */
	private void updateMusicInfoCache(String musicId) {
		try {
			// 从数据库查询最新数据（不要查缓存，避免数据不一致）
			MusicInfo musicInfo = this.musicInfoMapper.selectByMusicId(musicId);
			if (musicInfo != null) {
				// 补充用户信息
				if (musicInfo.getUserId() != null) {
					UserInfo userInfo = this.userInfoMapper.selectByUserId(musicInfo.getUserId());
					if (userInfo != null) {
						musicInfo.setNickName(userInfo.getNickName());
					}
				}
				// 更新Redis缓存（使用最新数据）
				redisComponent.saveMusicInfo(musicId, musicInfo);
			}
		} catch (Exception e) {
			log.error("更新音乐信息缓存失败, musicId={}", musicId, e);
			// 不抛出异常，避免影响主流程
		}
	}
}