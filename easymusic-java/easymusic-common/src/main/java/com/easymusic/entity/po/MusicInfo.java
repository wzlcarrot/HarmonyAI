package com.easymusic.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easymusic.entity.enums.DateTimePatternEnum;
import com.easymusic.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 音乐信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicInfo implements Serializable {

	/**
	 * 音乐ID
	 */
	private String musicId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 任务ID
	 */
	private String taskId;

	/**
	 * 创作ID
	 */
	private String creationId;

	/**
	 * 标题
	 */
	private String musicTitle;

	/**
	 * 封面
	 */
	private String cover;

	/**
	 * 音乐地址
	 */
	private String audioPath;

	/**
	 * 持续时间
	 */
	private Integer duration;

	/**
	 * 歌词
	 */
	private String lyrics;

	/**
	 * 播放数量
	 */
	private Integer playCount;

	/**
	 * 点赞数
	 */
	private Integer goodCount;

	/**
	 * 0:未推荐 1:已推荐
	 */
	private Integer commendType;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 0:生成音乐中 1:生成完毕
	 */
	private Integer musicStatus;

	/**
	 * 音乐类型 0:音乐 1:纯音乐
	 */
	private Integer musicType;

	private String avatar;

	private String nickName;

	private String actionId;

	private Boolean doGood;

}
