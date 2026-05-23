package com.easymusic.service;

import com.easymusic.entity.po.MusicInfo;
import com.easymusic.entity.query.MusicSearchQuery;
import com.easymusic.entity.vo.SearchResultVO;

import java.util.List;

/**
 * 音乐搜索服务接口
 * 
 * @author EasyMusic Team
 * @date 2024
 */
public interface MusicSearchService {

    /**
     * 根据音乐标题搜索音乐
     * 
     * @param query 搜索查询参数
     * @return 搜索结果
     */
    SearchResultVO search(MusicSearchQuery query);

    /**
     * 保存或更新音乐到ES
     * 
     * @param musicInfo 音乐信息
     */
    void saveOrUpdateMusicToES(MusicInfo musicInfo);

    /**
     * 批量保存音乐到ES（用于全量同步）
     * 
     * @param musicInfoList 音乐信息列表
     */
    void batchSaveMusicToES(List<MusicInfo> musicInfoList);

    /**
     * 从ES删除音乐
     * 
     * @param musicId 音乐ID
     */
    void deleteMusicFromES(String musicId);

    /**
     * 全量同步MySQL数据到ES（管理员功能）
     * 
     * @return 同步数量
     */
    Integer syncAllMusicToES();
}

