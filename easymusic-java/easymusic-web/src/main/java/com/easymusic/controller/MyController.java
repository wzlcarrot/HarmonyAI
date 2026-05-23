package com.easymusic.controller;

import com.easymusic.annotation.GlobalInterceptor;
import com.easymusic.annotation.RateLimit;

import com.easymusic.entity.dto.MusicSettingDTO;
import com.easymusic.entity.dto.TokenUserInfoDTO;

import com.easymusic.entity.po.MusicCreation;
import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.po.SysDict;
import com.easymusic.entity.po.UserIntegralRecord;
import com.easymusic.entity.query.MusicInfoQuery;
import com.easymusic.entity.query.UserIntegralRecordQuery;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.entity.enums.LimitType;
import com.easymusic.entity.enums.ResponseCodeEnum;
import com.easymusic.exception.BusinessException;

import com.easymusic.service.MusicCreationService;
import com.easymusic.service.MusicInfoService;
import com.easymusic.service.SysDictService;
import com.easymusic.service.UserIntegralRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@Validated
@RequestMapping("/my")
@RequiredArgsConstructor
public class MyController extends ABaseController{

    private final UserIntegralRecordService userIntegralRecordService;

    private final SysDictService sysDictService;

    private final MusicCreationService musicCreationService;

    private final MusicInfoService musicInfoService;

    @RequestMapping("integralRecords")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO integralRecords(Integer pageNo){

        UserIntegralRecordQuery param = new UserIntegralRecordQuery();
        param.setPageNo(pageNo);
        param.setUserId(getTokenUserInfoDTO().getUserId());
        param.setOrderBy("record_id desc");

        PaginationResultVO<UserIntegralRecord> result = userIntegralRecordService.findListByPage(param);

        return getSuccessResponseVO(result);
    }


    @RequestMapping("/loadSysDict")
    public ResponseVO loadSysDict(){
        Map<String, List<SysDict>> result = sysDictService.getDictList();

        return getSuccessResponseVO(result);
    }

    @RequestMapping("/loadMyMusic")
    @GlobalInterceptor(checkLogin = true)
    /*
    *    为什么使用queryLinkMusic这个参数。。因为在我的 喜欢的音乐页面中也使用了这个接口，，在那边queryLikeMusic是有值的，在这里是没有值的
    * */
    public ResponseVO loadMyMusic(Integer  pageNo,Boolean queryLikeMusic){
        log.info("pageNo:"+pageNo+" "+"queryLikeMusic:"+queryLikeMusic);
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
        if(tokenUserInfoDTO==null){
            return getSuccessResponseVO(new PaginationResultVO<>());
        }

        MusicInfoQuery musicInfoQuery = new MusicInfoQuery();
        musicInfoQuery.setPageNo(pageNo);
        musicInfoQuery.setOrderBy("create_time desc");
        if(queryLikeMusic!=null&&queryLikeMusic){
            musicInfoQuery.setQueryLikeMusic(true);
            musicInfoQuery.setLikeUserId(tokenUserInfoDTO.getUserId());
        } else{
            musicInfoQuery.setUserId(tokenUserInfoDTO.getUserId());
        }

        PaginationResultVO resultVO = musicInfoService.findListByPage(musicInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/createMusic")
    @GlobalInterceptor(checkLogin = true)
    @RateLimit(name = "music:create", rate = 5, interval = 60, limitType = LimitType.USER)
    public ResponseVO createMusic(@NotEmpty @Size(max = 500) String prompt,
                                  @Size(max = 1500) String lyrics,
                                  @NotNull Integer musicType,
                                  @NotEmpty String model,
                                  @Size(max = 200) String musicGener,
                                  @Size(max = 150) String musicEmotion,
                                  @Size(max = 5) String musicSex,
                                  @NotNull Integer modeType) {

        log.info("modeType:{}",modeType);
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
        MusicCreation musicCreation = new MusicCreation();
        musicCreation.setUserId(tokenUserInfoDTO.getUserId());
        musicCreation.setMusicType(musicType);
        musicCreation.setLyrics(lyrics);
        musicCreation.setPrompt(prompt);
        musicCreation.setModel(model);
        log.info("model:"+model);
        musicCreation.setModeType(modeType);


        log.info("createMusic接口创建成功");
        MusicSettingDTO musicSettingDTO = new MusicSettingDTO(musicGener, musicEmotion, musicSex);


        List<String> musicList = musicCreationService.createMusic(musicCreation,musicSettingDTO);
        return getSuccessResponseVO(musicList);
    }

    /*
    * 就是前端不断地调用loadcreatingmusic接口，从而通知调用后端loadcreatingmusic接口，主要是更新用户的创建的音乐状态。
    * 同时后端不断地调用getMusicFromQueue接口也就是处理任务
    * */
    @RequestMapping("/loadCreatingMusic")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadCreatingMusic(@NotEmpty String musicIds) {
        MusicInfoQuery musicInfoQuery = new MusicInfoQuery();
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
        musicInfoQuery.setUserId(tokenUserInfoDTO.getUserId());
        musicInfoQuery.setMusicIdList(Arrays.asList(musicIds.split(",")));
        List<MusicInfo> musicInfoList = musicInfoService.findListByParam(musicInfoQuery);
        return getSuccessResponseVO(musicInfoList);
    }

    @RequestMapping("/uploadMusicCover")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadMusicCover(@NotNull MultipartFile cover, @NotEmpty String musicId) {
        String resultCover = musicInfoService.updateCover(cover, getTokenUserInfoDTO().getUserId(), musicId);
        return getSuccessResponseVO(resultCover);
    }

    @RequestMapping("/changeMusicTitle")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO changeMusicTitle(@NotEmpty String musicId, @NotEmpty String musicTitle) {
        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setMusicTitle(musicTitle);
        Integer result = musicInfoService.updateMusicInfoByMusicId(musicInfo, musicId);
        log.info("result:{}",result);
        return getSuccessResponseVO(result);
    }


    @RequestMapping("/delMusic")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO delMusic(@NotEmpty String musicId) {

        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();

        // 先验证音乐是否属于当前用户
        MusicInfo musicInfo = musicInfoService.getMusicInfoByMusicId(musicId);
        if (musicInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!tokenUserInfoDTO.getUserId().equals(musicInfo.getUserId())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 使用deleteMusicInfoByMusicId方法，会同步删除MySQL、Redis和ES
        Integer result = musicInfoService.deleteMusicInfoByMusicId(musicId);
        log.info("result:{}",result);
        return getSuccessResponseVO(result+" "+"删除成功");
    }

}
