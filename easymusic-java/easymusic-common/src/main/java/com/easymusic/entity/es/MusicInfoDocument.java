package com.easymusic.entity.es;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;

/**
 * 音乐信息 ES 索引文档
 * 
 * @author EasyMusic Team
 * @date 2024
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "music_info")
public class MusicInfoDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 音乐ID（主键）
     */
    @Id
    private String musicId;

    /**
     * 用户ID
     */
    @Field(type = FieldType.Keyword)
    private String userId;

    /**
     * 音乐标题（支持中文分词搜索）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String musicTitle;

    /**
     * 用户昵称（用于搜索结果显示）
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String nickName;

    /**
     * 封面
     */
    @Field(type = FieldType.Keyword)
    private String cover;

    /**
     * 音频路径
     */
    @Field(type = FieldType.Keyword)
    private String audioPath;

    /**
     * 持续时间（秒）
     */
    @Field(type = FieldType.Integer)
    private Integer duration;

    /**
     * 播放数
     */
    @Field(type = FieldType.Integer)
    private Integer playCount;

    /**
     * 点赞数
     */
    @Field(type = FieldType.Integer)
    private Integer goodCount;

    /**
     * 推荐类型 0:未推荐 1:已推荐
     */
    @Field(type = FieldType.Integer)
    private Integer commendType;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date)
    private Date createTime;

    /**
     * 音乐状态 0:生成音乐中 1:生成完毕
     */
    @Field(type = FieldType.Integer)
    private Integer musicStatus;

    /**
     * 音乐类型 0:音乐 1:纯音乐
     */
    @Field(type = FieldType.Integer)
    private Integer musicType;
}

