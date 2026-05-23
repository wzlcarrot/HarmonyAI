package com.easymusic.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 音乐搜索查询参数
 * 
 * @author EasyMusic Team
 * @date 2024
 */
@Data
public class MusicSearchQuery extends BaseParam {

    /**
     * 搜索关键词（音乐标题）
     */
    @NotEmpty(message = "搜索关键词不能为空")
    private String keyword;

    /**
     * 音乐类型 0:音乐 1:纯音乐（可选）
     */
    private Integer musicType;

    /**
     * 排序类型 playCount-播放量（默认） time-最新发布
     */
    private String sortType;

    /**
     * 页码（从1开始）
     */
    @NotNull(message = "页码不能为空")
    private Integer pageNo;

    /**
     * 每页数量（默认20）
     */
    private Integer pageSize;

    /**
     * 默认构造函数，设置默认值
     */
    public MusicSearchQuery() {
        this.sortType = "playCount";
        this.pageSize = 20;
        this.pageNo = 1;
    }
}

