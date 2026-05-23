package com.easymusic.controller;

import java.util.List;

import com.easymusic.annotation.GlobalInterceptor;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.enums.CommendTypeEnum;
import com.easymusic.entity.enums.MusicStatusEnum;
import com.easymusic.entity.enums.PageSize;
import com.easymusic.entity.enums.ResponseCodeEnum;
import com.easymusic.entity.po.MusicCreation;
import com.easymusic.entity.po.MusicInfoAction;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.exception.BusinessException;
import com.easymusic.service.MusicCreationService;
import com.easymusic.service.MusicInfoActionService;
import com.easymusic.service.MusicInfoService;
import com.easymusic.service.MusicSearchService;
import com.easymusic.entity.query.MusicSearchQuery;
import com.easymusic.entity.vo.SearchResultVO;
import com.easymusic.redis.RedisComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;

/**
 * 音乐信息 Controller
 */
@RestController
@Slf4j
@Validated
@RequestMapping("/music")
@RequiredArgsConstructor
public class MusicInfoController extends ABaseController {

	private final MusicInfoService musicInfoService;

	private final MusicInfoActionService musicInfoActionService;

	private final MusicCreationService musicCreationService;

	private final RedisComponent redisComponent;

	private final MusicSearchService musicSearchService;

	@RequestMapping("/loadCommendMusic")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO loadCommendMusic() {
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();

		if (tokenUserInfoDTO == null) {
			throw new BusinessException(ResponseCodeEnum.CODE_901);
		}

		// 先从 Redis 中读取推荐列表
		String cacheKey = Constants.REDIS_KEY_MUSIC_COMMEND_LIST + tokenUserInfoDTO.getUserId();
		List<MusicInfo> list = redisComponent.getCommendMusicList(cacheKey);

		// 缓存命中则直接返回
		if (list != null && !list.isEmpty()) {
			return getSuccessResponseVO(list);
		}

		// 缓存未命中，走MySQL数据库查询
		MusicInfoQuery query = new MusicInfoQuery();
		query.setCommendType(CommendTypeEnum.COMMEND.getType());
		query.setOrderBy("m.create_time desc");
		query.setQueryUser(true);
		query.setCurrentUserId(tokenUserInfoDTO.getUserId());

		list = musicInfoService.findListByParam(query);

		// 查询结果写入 Redis
		if (list != null && !list.isEmpty()) {
			redisComponent.saveCommendMusicList(cacheKey, list, null);
		}

		return getSuccessResponseVO(list);
	}

	@RequestMapping("/loadLatestMusic")
	public ResponseVO loadLatestMusic(Integer pageNo, Integer indexType) {
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
		if(tokenUserInfoDTO==null){
			throw new BusinessException(ResponseCodeEnum.CODE_901);
		}
		if (pageNo == null || pageNo <= 0) {
			pageNo = 1;
		}

		MusicInfoQuery query = new MusicInfoQuery();
		query.setOrderBy("m.create_time desc");
		query.setQueryUser(true);
		query.setCommendType(CommendTypeEnum.NOT_COMMEND.getType());
		query.setMusicStatus(MusicStatusEnum.CREATED.getStatus());
		query.setCurrentUserId(tokenUserInfoDTO.getUserId());
		if (indexType != null) {
			query.setPageSize(PageSize.SIZE12.getSize());
		} else {
			query.setPageSize(PageSize.SIZE20.getSize());
		}

		query.setPageNo(pageNo);

		// 根据用户 + 页码 + indexType 组合缓存 key
		String cacheKey = Constants.REDIS_KEY_MUSIC_LATEST_PAGE
				+ tokenUserInfoDTO.getUserId() + ":"
				+ (indexType == null ? 0 : indexType) + ":"
				+ pageNo;

		// 先从 Redis 读取
		PaginationResultVO<MusicInfo> list = redisComponent.getLatestMusicPage(cacheKey);
		if (list != null) {
			return getSuccessResponseVO(list);
		}

		// 未命中缓存，再查数据库
		list = musicInfoService.findListByPage(query);

		// 只缓存非空结果
		if (list != null && list.getList() != null && !list.getList().isEmpty()) {
			redisComponent.saveLatestMusicPage(cacheKey, list, null);
		}

		return getSuccessResponseVO(list);
	}


	@RequestMapping("/musicDetail")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO musicDetail(@NotEmpty String musicId) {
		MusicInfo musicInfo = musicInfoService.getMusicInfoByMusicId(musicId);
		TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
		if(tokenUserInfoDTO==null){
			throw new BusinessException(ResponseCodeEnum.CODE_901);
		}

		MusicInfoAction action = musicInfoActionService.getMusicInfoActionByMusicIdAndUserId(musicId, tokenUserInfoDTO.getUserId());
		musicInfo.setDoGood(action != null);
		return getSuccessResponseVO(musicInfo);
	}


	@RequestMapping("/updatePlayCount")
	public ResponseVO updatePlayCount(@NotEmpty String musicId) {
		musicInfoService.updateMusicCount(musicId);
		return getSuccessResponseVO(null);
	}


	@RequestMapping("/doGood")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO doGood(@NotEmpty String musicId) {
		musicInfoActionService.doGood(musicId, getTokenUserInfoDTO().getUserId());
		return getSuccessResponseVO("点赞成功！");
	}


	@RequestMapping("/getCreation")
	public ResponseVO getCreation(@NotEmpty String creationId) {
		MusicCreation musicCreation = musicCreationService.getMusicCreationByCreationId(creationId);

		return getSuccessResponseVO(musicCreation);
	}

	/**
	 * 音乐搜索接口
	 * 
	 * 支持通过音乐标题搜索音乐，使用Elasticsearch实现全文搜索
	 * 
	 * @param query 搜索查询参数
	 * @return 搜索结果
	 */
	@RequestMapping("/search")
	public ResponseVO search(MusicSearchQuery query) {
		// 参数校验
		if (query == null || query.getKeyword() == null || query.getKeyword().trim().isEmpty()) {
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}
		
		// 设置默认值
		if (query.getPageNo() == null || query.getPageNo() <= 0) {
			query.setPageNo(1);
		}
		if (query.getPageSize() == null || query.getPageSize() <= 0) {
			query.setPageSize(20);
		}
		if (query.getSortType() == null || query.getSortType().trim().isEmpty()) {
			query.setSortType("playCount");
		}
		
		// 执行搜索
		SearchResultVO result = musicSearchService.search(query);
		
		return getSuccessResponseVO(result);
	}

}