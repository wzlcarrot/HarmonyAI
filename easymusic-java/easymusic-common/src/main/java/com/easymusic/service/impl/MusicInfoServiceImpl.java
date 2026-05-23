package com.easymusic.service.impl;

import java.util.List;
import java.util.Set;

import com.easymusic.api.MusicCreateApi;
import com.easymusic.entity.config.AppConfig;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.MusicCreationResultDTO;
import com.easymusic.entity.dto.MusicTaskDTO;
import com.easymusic.entity.enums.*;
import com.easymusic.entity.enums.ExecutorServiceSingletonEnum;
import com.easymusic.entity.po.UserInfo;
import com.easymusic.entity.query.UserInfoQuery;
import com.easymusic.exception.BusinessException;
import com.easymusic.mappers.UserInfoMapper;
import com.easymusic.mq.MusicCreateProducer;
import com.easymusic.redis.BloomFilterComponent;
import com.easymusic.redis.RedisComponent;
import com.easymusic.spring.SpringContext;
import com.easymusic.utils.FileUtils;
import com.easymusic.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.po.UserIntegralRecord;
import com.easymusic.entity.query.UserIntegralRecordQuery;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.mappers.MusicInfoMapper;
import com.easymusic.service.MusicInfoService;
import com.easymusic.service.MusicSearchService;
import com.easymusic.service.UserIntegralRecordService;
import com.easymusic.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 音乐信息 业务接口实现
 */
@Service("musicInfoService")
@Slf4j
@RequiredArgsConstructor
public class MusicInfoServiceImpl implements MusicInfoService {

	private final MusicInfoMapper<MusicInfo, MusicInfoQuery> musicInfoMapper;

	private final UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

	private final FileUtils fileUtils;

	private final AppConfig appConfig;

	private final RedisComponent redisComponent;

	private final RedissonClient redissonClient;

	private final MusicCreateProducer musicCreateProducer;

	private final BloomFilterComponent bloomFilterComponent;

	private final UserIntegralRecordService userIntegralRecordService;

	private final MusicSearchService musicSearchService;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MusicInfo> findListByParam(MusicInfoQuery param) {
		return this.musicInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MusicInfoQuery param) {
		return this.musicInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MusicInfo> findListByPage(MusicInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MusicInfo> list = this.findListByParam(param);
		PaginationResultVO<MusicInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MusicInfo bean) {
		return this.musicInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MusicInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.musicInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MusicInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.musicInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MusicInfo bean, MusicInfoQuery param) {
		StringTools.checkParam(param);
		return this.musicInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MusicInfoQuery param) {
		StringTools.checkParam(param);
		return this.musicInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据MusicId获取对象
	 *
	 * 使用布隆过滤器 + 两层缓存架构：Bloom -> Redis -> MySQL
	 * 读取顺序：布隆过滤器判断 -> Redis -> 数据库
	 */
	@Override
	public MusicInfo getMusicInfoByMusicId(String musicId) {
		// 0. 布隆过滤器快速判断，防止缓存穿透
		if (!bloomFilterComponent.mightContainMusic(musicId)) {
			log.debug("布隆过滤器判断音乐不存在, musicId={}", musicId);
			return null;
		}

		// 1. 先查 Redis（由 redisComponent.getMusicInfo 内部处理）
		MusicInfo musicInfo = redisComponent.getMusicInfo(musicId);
		if (musicInfo != null) {
			log.debug("从缓存获取音乐信息, musicId={}", musicId);
			// 补充用户信息
			if (musicInfo.getUserId() != null) {
				UserInfo userInfo = this.userInfoMapper.selectByUserId(musicInfo.getUserId());
				if (userInfo != null) {
					musicInfo.setNickName(userInfo.getNickName());
				}
			}
			return musicInfo;
		}

		// 2. Redis 未命中时，使用分布式锁防止缓存击穿
		String lockKey = "lock:music:" + musicId;
		RLock lock = redissonClient.getLock(lockKey);
		
		try {
			// 尝试获取锁，等待100ms，锁定3秒
			if (lock.tryLock(100, 3000, TimeUnit.MILLISECONDS)) {
				try {
					// 双重检查：再次查询 Redis（可能其他线程已经加载了）
					musicInfo = redisComponent.getMusicInfo(musicId);
					if (musicInfo != null) {
						// 补充用户信息
						if (musicInfo.getUserId() != null) {
							UserInfo userInfo = this.userInfoMapper.selectByUserId(musicInfo.getUserId());
							if (userInfo != null) {
								musicInfo.setNickName(userInfo.getNickName());
							}
						}
						return musicInfo;
					}
					
					// 查询数据库
					log.debug("缓存未命中，查询数据库, musicId={}", musicId);
					musicInfo = musicInfoMapper.selectByMusicId(musicId);
					
					if (musicInfo != null) {
						// 补充用户信息
						UserInfo userInfo = this.userInfoMapper.selectByUserId(musicInfo.getUserId());
						if (userInfo != null) {
							musicInfo.setNickName(userInfo.getNickName());
						}
						// 存入 Redis（会自动使用随机过期时间）
						redisComponent.saveMusicInfo(musicId, musicInfo);
						log.debug("音乐信息查询成功并缓存, musicId={}", musicId);
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
				musicInfo = redisComponent.getMusicInfo(musicId);
				// 如果还是没拿到，降级：直接查询数据库
				if (musicInfo == null) {
					musicInfo = musicInfoMapper.selectByMusicId(musicId);
					if (musicInfo != null) {
						// 补充用户信息
						UserInfo userInfo = this.userInfoMapper.selectByUserId(musicInfo.getUserId());
						if (userInfo != null) {
							musicInfo.setNickName(userInfo.getNickName());
						}
						redisComponent.saveMusicInfo(musicId, musicInfo);
					}
				} else {
					// 补充用户信息
					if (musicInfo.getUserId() != null) {
						UserInfo userInfo = this.userInfoMapper.selectByUserId(musicInfo.getUserId());
						if (userInfo != null) {
							musicInfo.setNickName(userInfo.getNickName());
						}
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("获取音乐信息锁被中断, musicId={}", musicId, e);
			// 降级：直接查询数据库
			musicInfo = musicInfoMapper.selectByMusicId(musicId);
			if (musicInfo != null) {
				// 补充用户信息
				UserInfo userInfo = this.userInfoMapper.selectByUserId(musicInfo.getUserId());
				if (userInfo != null) {
					musicInfo.setNickName(userInfo.getNickName());
				}
				redisComponent.saveMusicInfo(musicId, musicInfo);
			}
		}

		return musicInfo;
	}

	/**
	 * 根据MusicId修改
	 * 
	 * 同步流程：MySQL -> Redis -> ES
	 */
	@Override
	public Integer updateMusicInfoByMusicId(MusicInfo bean, String musicId) {
		// 1. 更新 MySQL（主数据源）
		Integer result = this.musicInfoMapper.updateByMusicId(bean, musicId);
		
		if (result > 0) {
			// 2. 直接从数据库查询最新数据（不要查缓存，避免数据不一致）
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
				
				// 3. 同步更新 ES 索引（新增功能）
				if (musicInfo.getMusicStatus() != null && musicInfo.getMusicStatus() == 1) {
					try {
						musicSearchService.saveOrUpdateMusicToES(musicInfo);
					} catch (Exception e) {
						log.error("更新ES失败, musicId={}", musicId, e);
						// 不抛出异常，避免影响主流程
					}
				}
			}
		}
		
		return result;
	}

	/**
	 * 根据MusicId删除
	 * 
	 * 同步流程：MySQL -> Redis -> ES
	 */
	@Override
	public Integer deleteMusicInfoByMusicId(String musicId) {
		// 1. 删除 MySQL
		Integer result = this.musicInfoMapper.deleteByMusicId(musicId);
		
		if (result > 0) {
			// 2. 删除 Redis 缓存（保持现有机制）
			try {
				redisComponent.deleteMusicInfo(musicId);
			} catch (Exception e) {
				log.warn("删除Redis缓存失败, musicId={}", musicId, e);
			}
			
			// 3. 从 ES 删除（新增功能）
			try {
				musicSearchService.deleteMusicFromES(musicId);
			} catch (Exception e) {
				log.error("从ES删除失败, musicId={}", musicId, e);
				// 不抛出异常，避免影响主流程
			}
		}
		
		return result;
	}

	/**
	 * 根据TaskId获取对象
	 */
	@Override
	public MusicInfo getMusicInfoByTaskId(String taskId) {
		return this.musicInfoMapper.selectByTaskId(taskId);
	}

	/**
	 * 根据TaskId修改
	 */
	@Override
	public Integer updateMusicInfoByTaskId(MusicInfo bean, String taskId) {
		return this.musicInfoMapper.updateByTaskId(bean, taskId);
	}

	/**
	 * 根据TaskId删除
	 */
	@Override
	public Integer deleteMusicInfoByTaskId(String taskId) {
		return this.musicInfoMapper.deleteByTaskId(taskId);
	}

	/**
	 * 更新播放数
	 * 
	 * 同步流程：MySQL -> Redis（可选） -> ES（用于排序）
	 */
	public void updateMusicCount(String musicId) {
		// 1. 更新 MySQL 播放数
		this.musicInfoMapper.updateMusicCount(musicId);
		
		// 2. 直接从数据库查询最新数据（不要查缓存，避免数据不一致）
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
			
			// 3. 更新 ES 索引（用于排序）
			if (musicInfo.getMusicStatus() != null && musicInfo.getMusicStatus() == 1) {
				try {
					musicSearchService.saveOrUpdateMusicToES(musicInfo);
				} catch (Exception e) {
					log.error("更新ES播放数失败, musicId={}", musicId, e);
					// 不抛出异常，避免影响主流程
				}
			}
		}
	}

	@Override
	public void musicCreateNotify(Integer musicType, String responseJson) {
		String apiCode;
		if(MusicTypeEnum.MUSIC.getType().equals(musicType)){
			apiCode = ModelType4MusicEnum.V3_5.getApiCode();
		}
		else if(MusicTypeEnum.PURE.getType().equals(musicType)){
			apiCode = ModelType4MusicEnum.V3_5.getApiCode();
		}
		else{
			throw new BusinessException("不支持的音乐类型:" + musicType);
		}

		//通过apiCode找到对应的实现bean
		MusicCreateApi musicCreateApi = (MusicCreateApi) SpringContext.getBean(apiCode);

		MusicCreationResultDTO resultDTO = musicCreateApi.createMusicNotify(musicType,responseJson);
		if(resultDTO==null){
			return;
		}
		musicCreated(resultDTO);

	}

	//其实图片是本地上传到服务器上的，这个方法的意思是把图片保存在指定的位置。。然后通过getResource方法来加载图片
	@Override
	public String updateCover(MultipartFile cover, String userId, String musicId) {

		MusicInfo musicInfo = this.musicInfoMapper.selectByMusicId(musicId);
		if (musicInfo == null || !musicInfo.getUserId().equals(userId)) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		String coverPath = fileUtils.uploadFile(cover) + "&" + System.currentTimeMillis();

		MusicInfo updateInfo = new MusicInfo();
		updateInfo.setCover(coverPath);
		musicInfoMapper.updateByMusicId(updateInfo, musicId);
		return coverPath;
	}

	//音乐创建完成。。更新 音乐信息，歌曲，歌词，音频
	private void musicCreated(MusicCreationResultDTO resultDTO){
		// 使用分布式锁防止重复更新音乐状态（基于taskId加锁）
		String lockKey = "lock:music:update:" + resultDTO.getTaskId();
		RLock lock = redissonClient.getLock(lockKey);
		
		try {
			// 尝试获取锁，等待3秒，锁定30秒
			if (lock.tryLock(3, 30, TimeUnit.SECONDS)) {
				try {
					// 先查询当前音乐状态，避免重复更新
					MusicInfoQuery checkQuery = new MusicInfoQuery();
					checkQuery.setTaskId(resultDTO.getTaskId());
					List<MusicInfo> existingMusic = musicInfoMapper.selectList(checkQuery);
					
					if (existingMusic != null && !existingMusic.isEmpty()) {
						MusicInfo music = existingMusic.get(0);
						// 如果已经是创建完成状态，直接返回，避免重复处理
						if (MusicStatusEnum.CREATED.getStatus().equals(music.getMusicStatus())) {
							log.info("音乐已创建完成，跳过重复更新, taskId={}, musicId={}", 
								resultDTO.getTaskId(), music.getMusicId());
							return;
						}
					}
					
					MusicInfo updateInfo = new MusicInfo();

					updateInfo.setMusicTitle(resultDTO.getTitle());
					updateInfo.setDuration(resultDTO.getDuration());

					String lyrics = JsonUtils.convertObj2Json(resultDTO.getLyricsList());
					updateInfo.setLyrics(lyrics);
					updateInfo.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
					String audioPath = resultDTO.getAudioUrl();
					updateInfo.setAudioPath(fileUtils.downloadFile(audioPath, ".mp3"));

					MusicInfoQuery query = new MusicInfoQuery();
					query.setTaskId(resultDTO.getTaskId());

					Integer count = musicInfoMapper.updateByParam(updateInfo,query);
					if(count==0){
						throw new BusinessException("音乐生成失败");
					}
					
					// 3. 同步更新 ES 索引（新增功能）
					// 查询更新后的完整音乐信息
					List<MusicInfo> updatedMusicList = musicInfoMapper.selectList(query);
					if (updatedMusicList != null && !updatedMusicList.isEmpty()) {
						MusicInfo updatedMusic = updatedMusicList.get(0);
						// 补充用户信息
						if (updatedMusic.getUserId() != null) {
							UserInfo userInfo = this.userInfoMapper.selectByUserId(updatedMusic.getUserId());
							if (userInfo != null) {
								updatedMusic.setNickName(userInfo.getNickName());
							}
						}
						// 更新Redis缓存
						redisComponent.saveMusicInfo(updatedMusic.getMusicId(), updatedMusic);
						// 将成功生成的音乐加入布隆过滤器，防止后续访问时穿透
						try {
							bloomFilterComponent.addMusic(updatedMusic.getMusicId());
						} catch (Exception e) {
							log.warn("音乐生成完成后加入布隆过滤器失败, musicId={}", updatedMusic.getMusicId(), e);
						}
						// 同步到ES（musicStatus=1时）
						if (updatedMusic.getMusicStatus() != null && updatedMusic.getMusicStatus() == 1) {
							try {
								musicSearchService.saveOrUpdateMusicToES(updatedMusic);
							} catch (Exception e) {
								log.error("同步音乐到ES失败, musicId={}", updatedMusic.getMusicId(), e);
								// 不抛出异常，避免影响主流程
							}
						}
					}
				} finally {
					// 释放锁
					if (lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				}
			} else {
				log.warn("获取音乐更新锁失败, taskId={}", resultDTO.getTaskId());
				throw new BusinessException("系统繁忙，请稍后重试");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("获取音乐更新锁被中断, taskId={}", resultDTO.getTaskId(), e);
			throw new BusinessException("获取锁被中断");
		}
	}

	/**
	 * 延迟队列轮询任务（Redis + MQ 混合方案）
	 * 
	 * 工作流程：
	 * 1. 每5秒轮询一次 Redis 延迟队列
	 * 2. 查找"到期任务"（已经等待了30秒的任务，可以开始查询音乐状态了）
	 * 3. 将到期任务发送到 RabbitMQ 进行实时处理
	 * 4. 从 Redis 延迟队列中移除已发送的任务
	 * 
	 * 什么是"到期任务"？
	 * - 用户创建音乐时，任务存入延迟队列，设置30秒后执行查询
	 * - 30秒后，这个任务就"到期"了，可以开始查询AI API看音乐是否生成完成
	 * - 到期任务 = 已经过了30秒等待期的普通音乐任务
	 * 
	 * 为什么使用 Redis 做延迟队列？
	 * - Redis ZSet 的 score 可以存储时间戳，天然支持按时间排序
	 * - 延迟精度要求不高（30秒），Redis 足够满足需求
	 * - 实现简单，无需额外的延迟消息插件
	 * 
	 * 为什么使用 MQ 做实时处理？
	 * - 消息可靠性高（持久化、ACK机制）
	 * - 支持消息重试和死信队列
	 * - 更好的削峰填谷能力
	 * - 解耦生产者和消费者
	 */
	@PostConstruct
	public void getMusicFromQueue() {
		if (!appConfig.getAutoCheckMusic()) {
			return;
		}
		ExecutorService executorService = ExecutorServiceSingletonEnum.INSTANCE.getExecutorService();
		executorService.execute(() -> {
			// 无限循环轮询延迟队列
			while (true) {
				try {
					// 从 Redis 延迟队列中获取到期任务
					// 到期任务 = 已经等待了30秒的任务（可以开始查询音乐状态了）
					Set<MusicTaskDTO> queueDataList = redisComponent.getMusicTaskDto();
					
					// 如果队列为空，等待5秒后继续下一次轮询
					//我感觉5s的意思是经验所得，如果是1s，可能导致轮训过于频繁，CPU压力比较大。
					//如果是30s，可能会导致延迟过高，影响用户体验。
					if (queueDataList == null || queueDataList.isEmpty()) {
						Thread.sleep(5000);
						continue;
					}
					
					// 遍历处理队列中的每个到期任务（已经过了30秒等待期的任务）
					for (MusicTaskDTO taskDto : queueDataList) {
						try {
							// 将到期的任务发送到 RabbitMQ 进行实时处理
							musicCreateProducer.sendMusicCreateTask(taskDto);
							
							// 从 Redis 延迟队列中移除已发送的任务
							redisComponent.removeMusicTaskDto(taskDto);
							
							log.info("延迟任务已发送到MQ, taskId={}, musicId={}", 
								taskDto.getTaskId(), taskDto.getMusicId());
						} catch (Exception e) {
							log.error("发送任务到MQ失败，任务保留在延迟队列中, taskId={}", 
								taskDto.getTaskId(), e);
							// 发送失败，任务保留在延迟队列中，下次轮询会重试
						}
					}
				} catch (Exception e) {
					log.error("获取延迟队列信息失败", e);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
						throw new RuntimeException(ex);
					}
				}
			}
		});
	}

	private void getMusicInfoFromAi(MusicTaskDTO taskDto) {
		// 先查询音乐信息，检查是否超时
		MusicInfo musicInfo = musicInfoMapper.selectByMusicId(taskDto.getMusicId());
		if (musicInfo == null) {
			log.warn("音乐信息不存在, musicId={}", taskDto.getMusicId());
			return;
		}

		// 检查音乐创建是否超时（30分钟）
		if (musicInfo.getCreateTime() != null) {
			long createTime = musicInfo.getCreateTime().getTime();
			long currentTime = System.currentTimeMillis();
			long diffMinutes = (currentTime - createTime) / (1000 * 60);
			
			// 如果超过30分钟仍未生成完成，标记为失败并退还积分
			if (diffMinutes >= 5 && MusicStatusEnum.CREATING.getStatus().equals(musicInfo.getMusicStatus())) {
				log.warn("音乐创建超时, musicId={}, taskId={}, 创建时间={}, 已等待{}分钟", 
					taskDto.getMusicId(), taskDto.getTaskId(), musicInfo.getCreateTime(), diffMinutes);
				markMusicAsFailedAndRefundIntegral(musicInfo);
				return;
			}
		}

		//根据API来获取对应的实现类
		MusicCreateApi musicCreateApi = (MusicCreateApi) SpringContext.getBean(taskDto.getApiCode());
		MusicCreationResultDTO resultDTO = null;

		//如果当前任务元素是普通音乐类型，则查询是否音乐存在并且返回结果。
		if(MusicTypeEnum.MUSIC.getType().equals(taskDto.getMusicType())){
			resultDTO = musicCreateApi.musicQuery(taskDto.getTaskId());
		}
		else if(MusicTypeEnum.PURE.getType().equals(taskDto.getMusicType())){
			resultDTO = musicCreateApi.pureMusicQuery(taskDto.getTaskId());
		}

		//这种设计是为了处理API查询失败或音乐尚未生成完成的情况，通过重新将任务放回队列实现重试机制
		if(resultDTO==null){
			// 再次检查是否超时，如果超时则不再重试
			if (musicInfo.getCreateTime() != null) {
				long createTime = musicInfo.getCreateTime().getTime();
				long currentTime = System.currentTimeMillis();
				long diffMinutes = (currentTime - createTime) / (1000 * 60);
				
				if (diffMinutes >= 5 && MusicStatusEnum.CREATING.getStatus().equals(musicInfo.getMusicStatus())) {
					log.warn("音乐创建超时，不再重试, musicId={}, taskId={}, 已等待{}分钟", 
						taskDto.getMusicId(), taskDto.getTaskId(), diffMinutes);
					markMusicAsFailedAndRefundIntegral(musicInfo);
					return;
				}
			}
			redisComponent.addMusicCreateTask(taskDto);
			return;
		}

		musicCreated(resultDTO);

	}

	/**
	 * 标记音乐为失败状态并退还积分
	 */
	private void markMusicAsFailedAndRefundIntegral(MusicInfo musicInfo) {
		try {
			// 更新音乐状态为失败
			MusicInfo updateInfo = new MusicInfo();
			updateInfo.setMusicStatus(MusicStatusEnum.CREATE_FAIL.getStatus());
			MusicInfoQuery query = new MusicInfoQuery();
			query.setMusicId(musicInfo.getMusicId());
			musicInfoMapper.updateByParam(updateInfo, query);
			
			// 退还积分（使用musicId查询该音乐对应的积分记录）
			if (musicInfo.getMusicId() != null && musicInfo.getUserId() != null) {
				// 查询该音乐ID对应的积分记录（每个音乐单独扣积分，使用musicId作为businessId）
				UserIntegralRecordQuery integralQuery = new UserIntegralRecordQuery();
				integralQuery.setBusinessId(musicInfo.getMusicId());
				integralQuery.setRecordType(UserIntegralRecordTypeEnum.CREATE_MUSIC.getType());
				integralQuery.setUserId(musicInfo.getUserId());
				
				List<UserIntegralRecord> records = userIntegralRecordService.findListByParam(integralQuery);
				
				if (records != null && !records.isEmpty()) {
					// 检查该音乐是否已经退还过积分，避免重复退还
					UserIntegralRecordQuery refundQuery = new UserIntegralRecordQuery();
					refundQuery.setBusinessId(musicInfo.getMusicId());
					refundQuery.setRecordType(UserIntegralRecordTypeEnum.CREATE_MUSIC_BACK.getType());
					refundQuery.setUserId(musicInfo.getUserId());
					
					List<UserIntegralRecord> refundRecords = userIntegralRecordService.findListByParam(refundQuery);
					
					// 如果还没有退还过积分，则退还该音乐对应的积分
					if (refundRecords == null || refundRecords.isEmpty()) {
						UserIntegralRecord firstRecord = records.get(0);
						Integer integralToRefund = Math.abs(firstRecord.getChangeIntegral());
						
						log.info("音乐创建失败，开始退还积分, musicId={}, userId={}, integral={}", 
							musicInfo.getMusicId(), musicInfo.getUserId(), integralToRefund);
						
						userIntegralRecordService.changeUserIntegral(
							UserIntegralRecordTypeEnum.CREATE_MUSIC_BACK,
							musicInfo.getMusicId(),  // 使用musicId作为businessId
							musicInfo.getUserId(),
							integralToRefund,
							null
						);
						
						log.info("积分退还成功, musicId={}, userId={}, integral={}", 
							musicInfo.getMusicId(), musicInfo.getUserId(), integralToRefund);
					} else {
						log.info("该音乐已退还过积分，跳过重复退还, musicId={}", 
							musicInfo.getMusicId());
					}
				} else {
					log.warn("未找到该音乐的积分记录，无法退还, musicId={}, userId={}", 
						musicInfo.getMusicId(), musicInfo.getUserId());
				}
			}
		} catch (Exception e) {
			log.error("标记音乐失败或退还积分时发生异常, musicId={}, creationId={}, userId={}", 
				musicInfo.getMusicId(), musicInfo.getCreationId(), musicInfo.getUserId(), e);
		}
	}

	/**
	 * 处理音乐创建任务（供 MQ 消费者调用）
	 * 这个方法会被 MQ 消费者调用，用于处理从消息队列中接收到的音乐创建任务
	 */
	@Override
	public void processMusicCreateTask(MusicTaskDTO taskDto) {
		// 复用原有的逻辑
		getMusicInfoFromAi(taskDto);
	}

}