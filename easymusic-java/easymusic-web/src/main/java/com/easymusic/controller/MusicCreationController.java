package com.easymusic.controller;

import java.util.List;

import com.easymusic.annotation.GlobalInterceptor;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.query.MusicCreationQuery;
import com.easymusic.entity.po.MusicCreation;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.exception.BusinessException;
import com.easymusic.service.MusicCreationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 音乐创作信息 Controller
 */
@RestController("musicCreationController")
@RequestMapping("/musicCreation")
@RequiredArgsConstructor
@GlobalInterceptor(checkLogin = true)
public class MusicCreationController extends ABaseController{

	private final MusicCreationService musicCreationService;
	/**
	 * 根据条件分页查询
	 */
	@RequestMapping("/loadDataList")
	public ResponseVO loadDataList(MusicCreationQuery query){
		return getSuccessResponseVO(musicCreationService.findListByPage(query));
	}

	/**
	 * 新增
	 */
	@RequestMapping("/add")
	public ResponseVO add(MusicCreation bean) {
		musicCreationService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增
	 */
	@RequestMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<MusicCreation> listBean) {
		musicCreationService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增/修改
	 */
	@RequestMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<MusicCreation> listBean) {
		musicCreationService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据CreationId查询对象
	 */
	@RequestMapping("/getMusicCreationByCreationId")
	public ResponseVO getMusicCreationByCreationId(String creationId) {
		return getSuccessResponseVO(musicCreationService.getMusicCreationByCreationId(creationId));
	}

	/**
	 * 根据CreationId修改对象
	 */
	@RequestMapping("/updateMusicCreationByCreationId")
	public ResponseVO updateMusicCreationByCreationId(MusicCreation bean,String creationId) {
		musicCreationService.updateMusicCreationByCreationId(bean,creationId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据CreationId删除
	 */
	@RequestMapping("/deleteMusicCreationByCreationId")
	public ResponseVO deleteMusicCreationByCreationId(String creationId) {
		musicCreationService.deleteMusicCreationByCreationId(creationId);
		return getSuccessResponseVO(null);
	}

}