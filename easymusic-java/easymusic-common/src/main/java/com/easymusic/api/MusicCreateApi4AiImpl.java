package com.easymusic.api;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.easymusic.entity.config.AppConfig;
import com.easymusic.entity.dto.MusicCreationResultDTO;
import com.easymusic.entity.enums.MusicTypeEnum;
import com.easymusic.utils.JsonUtils;
import com.easymusic.utils.OKHttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Component("tianpuyueApi")
public class MusicCreateApi4AiImpl implements MusicCreateApi{
    /**
     * 生成音乐
     */
    private String URL_CREATE_MUSIC = "/open-apis/v1/song/generate";
    /**
     * 查询音乐生成
     */
    private String URL_QUERY_MUSIC = "/open-apis/v1/song/query";

    /**
     * 生成纯音乐
     */
    private String URL_CREATE_PURE_MUSIC = "/open-apis/v1/instrumental/generate";

    private String URL_QUERY_PURE_MUSIC = "/open-apis/v1/instrumental/query";

    private String CALL_BACL_URL = "/api/musicNotify/tianpuyu/%d";

    @Resource
    private AppConfig appConfig;

    private Map<String,String> getHeaders(){
        Map<String,String> header = new HashMap<>();
        header.put("Content-Type","application/json; charset=utf-8");
        header.put("Authorization",appConfig.getTianpuyueApiKey());
        header.put("courseOrderId", appConfig.getTianpuyueApiCourseOrderId());
        return header;
    }

    //这个方法实现了向第三方音乐生成 API 发送请求，并处理响应的过程
    public List<String> createMusic(String model, String prompt, String lyrics){
        Map<String, String> header = getHeaders();
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);
        params.put("lyrics", lyrics);
        params.put("model", model);
        params.put("callback_url", appConfig.getWebDomain() + String.format(CALL_BACL_URL, MusicTypeEnum.MUSIC.getType()));  //创建回调地址
        String jsonParams = JsonUtils.convertObj2Json(params);
        String response = OKHttpUtils.postRequest4Json(appConfig.getTianpuyueApiDomain() + URL_CREATE_MUSIC, header, jsonParams);
        List<String> itemList = (List<String>) JSONPath.eval(response, "$.data.item_ids");
        return itemList;
    }
    //查询第三方音乐生成 API 中指定音乐项的详细信息
    public MusicCreationResultDTO musicQuery(String itemId) {
        //获取请求所需的头部信息
        Map<String, String> header = getHeaders();
        Map<String, Object> params = new HashMap<>();
        //创建请求参数对象，包含要查询的音乐项 ID
        params.put("item_ids", new String[]{itemId});
        //将参数转换为 JSON 字符串
        String jsonParams = JsonUtils.convertObj2Json(params);
        //向第三方api发送post请求
        String response = OKHttpUtils.postRequest4Json(appConfig.getTianpuyueApiDomain() + URL_QUERY_MUSIC, header, jsonParams);
        //解析响应数据
        JSONObject jsonObject = (JSONObject) JSONPath.eval(response, "$.data.songs[0]");
        Integer status = (Integer) JSONPath.eval(response, "$.status");

        //转化成歌曲数据，并且进行返回
        return getMusicResultDTO(jsonObject);
    }


    private MusicCreationResultDTO getMusicResultDTO(JSONObject jsonObject) {

        if (jsonObject == null) {
            return null;
        }
        //表示没有歌词，则返回 null。
        if (jsonObject.get("lyrics_sections") == null) {
            return null;
        }
        //将JSON对象中的歌词部分转换为Lyrics对象列表
        List<MusicCreationResultDTO.Lyrics> lyricsList = JsonUtils.convertJsonArray2List(JsonUtils.convertObj2Json(jsonObject.get("lyrics_sections")),
                MusicCreationResultDTO.Lyrics.class);

        MusicCreationResultDTO resultDTO = new MusicCreationResultDTO();

        log.info("audio_url:"+jsonObject.getString("audio_url")+" "+"audio_hi_url:"+jsonObject.getString("audio_hi_url")+" "+"duration:"+jsonObject.getIntValue("duration"));
        resultDTO.setTaskId(jsonObject.getString("item_id"));
        resultDTO.setTitle(jsonObject.getString("title"));
        resultDTO.setAudioUrl(jsonObject.getString("audio_url"));
        resultDTO.setAudioHiUrl(jsonObject.getString("audio_hi_url"));
        resultDTO.setDuration(jsonObject.getIntValue("duration"));
        resultDTO.setLyricsList(lyricsList);
        resultDTO.setCreateSuccess(true);
        return resultDTO;
    }
    //这个方法实现了向第三方音乐生成 API 发送请求，并处理响应的过程。其实主要响应过程就是生成纯音乐
    public List<String> createPureMusic(String model,String prompt){
        Map<String, String> header = getHeaders();
        Map<String, Object> params = new HashMap<>();
        params.put("prompt", prompt);
        params.put("model", model);
        params.put("callback_url", appConfig.getWebDomain() + String.format(CALL_BACL_URL, MusicTypeEnum.PURE.getType()));  //创建回调地址
        String jsonParams = JsonUtils.convertObj2Json(params);
        String response = OKHttpUtils.postRequest4Json(appConfig.getTianpuyueApiDomain() + URL_CREATE_PURE_MUSIC, header, jsonParams);
        List<String> itemList = (List<String>) JSONPath.eval(response, "$.data.item_ids");
        return itemList;
    }

    //通过itemId查询纯音乐
    public MusicCreationResultDTO pureMusicQuery(String itemId){
        //获取请求所需的头部信息
        Map<String, String> header = getHeaders();
        Map<String, Object> params = new HashMap<>();
        //创建请求参数对象，包含要查询的音乐项 ID
        params.put("item_ids", new String[]{itemId});
        //将参数转换为 JSON 字符串
        String jsonParams = JsonUtils.convertObj2Json(params);
        //向第三方api发送post请求
        String response = OKHttpUtils.postRequest4Json(appConfig.getTianpuyueApiDomain() + URL_QUERY_PURE_MUSIC, header, jsonParams);
        //解析响应数据
        JSONObject jsonObject = (JSONObject) JSONPath.eval(response, "$.data.instrumentals[0]");
        Integer status = (Integer) JSONPath.eval(response, "$.status");

        //转化成歌曲数据，并且进行返回
        return getPureMusicResultDTO(jsonObject);
    }

    private MusicCreationResultDTO getPureMusicResultDTO(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        MusicCreationResultDTO resultDTO = new MusicCreationResultDTO();
        resultDTO.setTaskId(jsonObject.getString("item_id"));
        resultDTO.setAudioUrl(jsonObject.getString("audio_url"));
        resultDTO.setAudioHiUrl(jsonObject.getString("audio_hi_url"));
        resultDTO.setDuration(jsonObject.getIntValue("duration"));
        resultDTO.setCreateSuccess(true);
        return resultDTO;
    }
    //创建音乐通知结果
    public MusicCreationResultDTO createMusicNotify(Integer musicType, String responseBody) {
        //获取音乐类型
        MusicTypeEnum musicTypeEnum = MusicTypeEnum.getByType(musicType);

        //如果是普通音乐
        if (MusicTypeEnum.MUSIC == musicTypeEnum) {
            JSONObject jsonObject = (JSONObject) JSONPath.eval(responseBody, "$.songs[0]");
            return getMusicResultDTO(jsonObject);
            //如果是纯音乐
        } else if (MusicTypeEnum.PURE == musicTypeEnum) {
            JSONObject jsonObject = (JSONObject) JSONPath.eval(responseBody, "$.instrumentals[0]");
            return getPureMusicResultDTO(jsonObject);
        }
        return null;
    }
}
