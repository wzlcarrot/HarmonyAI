package com.easymusic.controller;

import java.util.List;

import com.easymusic.entity.query.MusicInfoActionQuery;
import com.easymusic.entity.po.MusicInfoAction;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.service.MusicInfoActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 音乐操作 Controller
 */
@RestController("musicInfoActionController")
@RequestMapping("/musicInfoAction")
@RequiredArgsConstructor
public class MusicInfoActionController extends ABaseController{

	private final MusicInfoActionService musicInfoActionService;
	/**
	 * 根据条件分页查询
	 */
	@RequestMapping("/loadDataList")
	public ResponseVO loadDataList(MusicInfoActionQuery query){
		return getSuccessResponseVO(musicInfoActionService.findListByPage(query));
	}

	/**
	 * 新增
	 */
	@RequestMapping("/add")
	public ResponseVO add(MusicInfoAction bean) {
		musicInfoActionService.add(bean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增
	 */
	@RequestMapping("/addBatch")
	public ResponseVO addBatch(@RequestBody List<MusicInfoAction> listBean) {
		musicInfoActionService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 批量新增/修改
	 */
	@RequestMapping("/addOrUpdateBatch")
	public ResponseVO addOrUpdateBatch(@RequestBody List<MusicInfoAction> listBean) {
		musicInfoActionService.addBatch(listBean);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据ActionId查询对象
	 */
	@RequestMapping("/getMusicInfoActionByActionId")
	public ResponseVO getMusicInfoActionByActionId(Integer actionId) {
		return getSuccessResponseVO(musicInfoActionService.getMusicInfoActionByActionId(actionId));
	}

	/**
	 * 根据ActionId修改对象
	 */
	@RequestMapping("/updateMusicInfoActionByActionId")
	public ResponseVO updateMusicInfoActionByActionId(MusicInfoAction bean,Integer actionId) {
		musicInfoActionService.updateMusicInfoActionByActionId(bean,actionId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据ActionId删除
	 */
	@RequestMapping("/deleteMusicInfoActionByActionId")
	public ResponseVO deleteMusicInfoActionByActionId(Integer actionId) {
		musicInfoActionService.deleteMusicInfoActionByActionId(actionId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据MusicIdAndUserId查询对象
	 */
	@RequestMapping("/getMusicInfoActionByMusicIdAndUserId")
	public ResponseVO getMusicInfoActionByMusicIdAndUserId(String musicId,String userId) {
		return getSuccessResponseVO(musicInfoActionService.getMusicInfoActionByMusicIdAndUserId(musicId,userId));
	}

	/**
	 * 根据MusicIdAndUserId修改对象
	 */
	@RequestMapping("/updateMusicInfoActionByMusicIdAndUserId")
	public ResponseVO updateMusicInfoActionByMusicIdAndUserId(MusicInfoAction bean,String musicId,String userId) {
		musicInfoActionService.updateMusicInfoActionByMusicIdAndUserId(bean,musicId,userId);
		return getSuccessResponseVO(null);
	}

	/**
	 * 根据MusicIdAndUserId删除
	 */
	@RequestMapping("/deleteMusicInfoActionByMusicIdAndUserId")
	public ResponseVO deleteMusicInfoActionByMusicIdAndUserId(String musicId,String userId) {
		musicInfoActionService.deleteMusicInfoActionByMusicIdAndUserId(musicId,userId);
		return getSuccessResponseVO(null);
	}
}