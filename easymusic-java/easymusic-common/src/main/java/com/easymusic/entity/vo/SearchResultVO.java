package com.easymusic.entity.vo;

import com.easymusic.entity.po.MusicInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果VO
 * 
 * @author EasyMusic Team
 * @date 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVO {

    /**
     * 当前页码
     */
    private Integer pageNo;

    /**
     * 每页数量
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer pageTotal;

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 音乐列表
     */
    private List<MusicInfo> musicList;

    /**
     * 构造函数
     */
    public SearchResultVO(Integer pageNo, Integer pageSize, Long totalCount, List<MusicInfo> musicList) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.musicList = musicList != null ? musicList : new ArrayList<>();
        
        // 计算总页数
        if (pageSize != null && pageSize > 0 && totalCount != null) {
            this.pageTotal = (int) Math.ceil((double) totalCount / pageSize);
        } else {
            this.pageTotal = 0;
        }
    }
}

