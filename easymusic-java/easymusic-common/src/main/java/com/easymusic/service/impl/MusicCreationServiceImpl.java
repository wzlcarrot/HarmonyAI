package com.easymusic.service.impl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.*;

import com.easymusic.api.MusicCreateApi;

import com.easymusic.entity.config.AppConfig;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.dto.MusicSettingDTO;
import com.easymusic.entity.dto.MusicTaskDTO;
import com.easymusic.entity.enums.*;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.po.SysDict;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.query.SysDictQuery;
import com.easymusic.exception.BusinessException;
import com.easymusic.mappers.MusicInfoMapper;
import com.easymusic.mappers.SysDictMapper;
import com.easymusic.redis.RedisComponent;
import com.easymusic.service.UserIntegralRecordService;
import com.easymusic.spring.SpringContext;
import com.easymusic.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.easymusic.entity.query.MusicCreationQuery;
import com.easymusic.entity.po.MusicCreation;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.mappers.MusicCreationMapper;
import com.easymusic.service.MusicCreationService;
import com.easymusic.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.redisson.api.RedissonClient;


/**
 * 音乐创作信息 业务接口实现
 */
@Service("musicCreationService")
@Slf4j
@RequiredArgsConstructor
public class MusicCreationServiceImpl implements MusicCreationService {

	private final MusicCreationMapper<MusicCreation, MusicCreationQuery> musicCreationMapper;

	private final SysDictMapper<SysDict, SysDictQuery> sysDictMapper;

	private final UserIntegralRecordService userIntegralRecordService;

	private final AppConfig appConfig;

	private final RedisComponent redisComponent;

	private final MusicInfoMapper<MusicInfo, MusicInfoQuery> musicInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MusicCreation> findListByParam(MusicCreationQuery param) {
		return this.musicCreationMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MusicCreationQuery param) {
		return this.musicCreationMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MusicCreation> findListByPage(MusicCreationQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MusicCreation> list = this.findListByParam(param);
		PaginationResultVO<MusicCreation> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MusicCreation bean) {
		return this.musicCreationMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MusicCreation> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.musicCreationMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MusicCreation> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.musicCreationMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MusicCreation bean, MusicCreationQuery param) {
		StringTools.checkParam(param);
		return this.musicCreationMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MusicCreationQuery param) {
		StringTools.checkParam(param);
		return this.musicCreationMapper.deleteByParam(param);
	}

	/**
	 * 根据CreationId获取对象
	 */
	@Override
	public MusicCreation getMusicCreationByCreationId(String creationId) {
		return this.musicCreationMapper.selectByCreationId(creationId);
	}

	/**
	 * 根据CreationId修改
	 */
	@Override
	public Integer updateMusicCreationByCreationId(MusicCreation bean, String creationId) {
		return this.musicCreationMapper.updateByCreationId(bean, creationId);
	}

	/**
	 * 根据CreationId删除
	 */
	@Override
	public Integer deleteMusicCreationByCreationId(String creationId) {
		return this.musicCreationMapper.deleteByCreationId(creationId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public List<String> createMusic(MusicCreation musicCreation, MusicSettingDTO musicSettingDTO) {

		MusicTypeEnum musicTypeEnum = MusicTypeEnum.getByType(musicCreation.getMusicType());
		if (musicTypeEnum == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		ModelInfo modelInfo = getModelInfo(musicTypeEnum, musicCreation.getModel());
		String model = modelInfo.model;
		log.info("model:" + model);
		SysDictQuery query = new SysDictQuery();
		//使用pCode的意思是，举例，pCode是music_model,code是v3或者v3.5.有点像树状结构。
		query.setDictPcode(musicTypeEnum.getDictCode());
		List<SysDict> sysDictSubList = redisComponent.getDictSubList(musicTypeEnum.getDictCode());

		// 如果Redis中没有数据，则从数据库查询并同步到Redis
		if (sysDictSubList == null || sysDictSubList.isEmpty()) {
			SysDictQuery dictQuery = new SysDictQuery();
			dictQuery.setDictPcode(musicTypeEnum.getDictCode());
			dictQuery.setOrderBy("sort asc");
			sysDictSubList = sysDictMapper.selectList(dictQuery);

			// 同步到Redis
			if (sysDictSubList != null && !sysDictSubList.isEmpty()) {
				redisComponent.saveDict(musicTypeEnum.getDictCode(), sysDictSubList);
			}
		}

		SysDict sysDict = null;
		for (SysDict dict : sysDictSubList) {
			if (dict.getDictCode().equals(musicCreation.getModel())) {
				sysDict = dict;
				break;
			}
		}

		if (sysDict == null) {
			throw new BusinessException("系统配置错误，请联系管理员");
		}

		String creationId = StringTools.getRandomNumber(Constants.LENGTH_15);

		Integer integral = Integer.parseInt(sysDict.getDictValue());
		String apiCode = modelInfo.apiCode;

		Date curDate = new Date();
		musicCreation.setCreationId(creationId);
		musicCreation.setModel(model);
		musicCreation.setCreateTime(curDate);
		musicCreation.setSettings(JsonUtils.convertObj2Json(musicSettingDTO));

		musicCreationMapper.insert(musicCreation);

			String prompt = musicCreation.getPrompt();
			/*
				我知道了，性质不一样，一个是在方法内的局部变量，一个是实例变量，
				方法内的局部变量在栈中，每个栈每个线程都有的，实例变量在堆内存中，各个线程可以共享的。
			
			*/
			//如果是高级模式，则提示词需要更加专业一点
			if (MusicModeTypeEnum.ADVANCED.getModeType().equals(musicCreation.getModeType())) {
				if (musicSettingDTO != null) {
					StringBuilder promptBuilder = new StringBuilder(prompt);

					if (musicSettingDTO.getMusicGener() != null) {
						promptBuilder.append(" 曲风:").append(musicSettingDTO.getMusicGener());
					}
					if (musicSettingDTO.getMusicEmotion() != null) {
						promptBuilder.append(" 情绪:").append(musicSettingDTO.getMusicEmotion());
					}
					if (musicSettingDTO.getMusicSex() != null) {
						promptBuilder.append(" 人声:").append(musicSettingDTO.getMusicSex());
					}

					prompt = promptBuilder.toString();
				}
				else{
					throw new BusinessException("系统配置错误");
				}
			}

			//通过apiCode拿到最终的api对应的实现类
			MusicCreateApi musicCreateApi = (MusicCreateApi) SpringContext.getBean(apiCode);
			List<String> itemIds;
			//好像createMusic方法调用了天乐谱的api然后返回得到itemId音乐列表，并不是说一定就创建成功了
			// 此时音乐正在后台生成，状态为"创建中"
			// 需要后续通过回调或轮询来获取生成结果
			if (MusicTypeEnum.MUSIC.getType().equals(musicCreation.getMusicType())) {
				itemIds = musicCreateApi.createMusic(model, prompt, musicCreation.getLyrics());  //调用了实现类的createMusic方法
			} else {
				itemIds = musicCreateApi.createPureMusic(model, prompt);
			}

			if (itemIds == null || itemIds.isEmpty()) {
				throw new BusinessException("音乐创建失败");
			}

			List<MusicInfo> musicInfoList = new ArrayList<>();
			List<String> musicIdList = new ArrayList<>();
			List<String> failedMusicIds = new ArrayList<>(); // 记录扣积分失败的音乐ID
			log.info("itemIds:" + itemIds);  //音乐id列表

			// 为每个音乐单独扣减积分
			for (String itemId : itemIds) {
				MusicInfo musicInfo = new MusicInfo();
				String musicId = StringTools.getRandomNumber(Constants.LENGTH_12);
				musicInfo.setMusicId(musicId);
				musicInfo.setUserId(musicCreation.getUserId());
				musicInfo.setCreationId(musicCreation.getCreationId());
				musicInfo.setGoodCount(0);
				musicInfo.setPlayCount(0);
				musicInfo.setCreateTime(curDate);
				musicInfo.setCommendType(CommendTypeEnum.NOT_COMMEND.getType());
				musicInfo.setMusicStatus(MusicStatusEnum.CREATING.getStatus());  //创建音乐中
				musicInfo.setTaskId(itemId);
				musicInfo.setMusicType(musicTypeEnum.getType());

				// 为每个音乐单独扣减积分（使用musicId作为businessId）
				try {
					userIntegralRecordService.changeUserIntegral(
						UserIntegralRecordTypeEnum.CREATE_MUSIC, 
						musicId, 
						musicCreation.getUserId(), 
						-integral, 
						null);
					log.info("音乐积分扣减成功, musicId={}, userId={}, integral={}", 
						musicId, musicCreation.getUserId(), integral);
				} catch (Exception e) {
					log.error("音乐积分扣减失败, musicId={}, userId={}, integral={}", 
						musicId, musicCreation.getUserId(), integral, e);
					failedMusicIds.add(musicId);
					// 积分扣减失败，标记该音乐为失败状态
					musicInfo.setMusicStatus(MusicStatusEnum.CREATE_FAIL.getStatus());
				}

				musicInfoList.add(musicInfo);

				// 只有积分扣减成功的音乐才加入任务队列
				if (appConfig.getAutoCheckMusic() && !failedMusicIds.contains(musicId)) {
					MusicTaskDTO taskDTO = new MusicTaskDTO();
					taskDTO.setApiCode(apiCode);
					taskDTO.setMusicId(musicInfo.getMusicId());
					taskDTO.setTaskId(itemId);
					taskDTO.setMusicType(musicCreation.getMusicType());
					redisComponent.addMusicCreateTask(taskDTO);
				}

				musicIdList.add(musicInfo.getMusicId());
			}

			// 批量插入音乐信息
			musicInfoMapper.insertBatch(musicInfoList);
			
			// 如果有积分扣减失败的音乐，需要记录日志
			if (!failedMusicIds.isEmpty()) {
				log.warn("部分音乐积分扣减失败, failedMusicIds={}, total={}", 
					failedMusicIds, itemIds.size());
			}
			
			log.info("创建音乐成功, 共{}个音乐, 成功扣积分{}个", itemIds.size(), itemIds.size() - failedMusicIds.size());
			return musicIdList;

	}

	class ModelInfo {
		String model;
		String apiCode;

		public ModelInfo(String model, String apiCode) {
			this.model = model;
			this.apiCode = apiCode;
		}
	}

	private ModelInfo getModelInfo(MusicTypeEnum musicTypeEnum, String modelId) {
		if (MusicTypeEnum.MUSIC == musicTypeEnum) {
			//看看具体是选择什么模型
			ModelType4MusicEnum modelType4MusicEnum = ModelType4MusicEnum.getById(modelId);

			if (modelType4MusicEnum == null) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}

			return new ModelInfo(modelType4MusicEnum.getModelCode(), modelType4MusicEnum.getApiCode());
		} else if (MusicTypeEnum.PURE == musicTypeEnum) {
			ModelType4PureMusicEnum modelType4PureMusicEnum = ModelType4PureMusicEnum.getById(modelId);
			if (modelType4PureMusicEnum == null) {
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
			return new ModelInfo(modelType4PureMusicEnum.getModelCode(), modelType4PureMusicEnum.getApiCode());
		} else {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

	}

}