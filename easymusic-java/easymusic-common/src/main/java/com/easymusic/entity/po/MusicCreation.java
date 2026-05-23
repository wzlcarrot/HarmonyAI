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
 * 音乐创作信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicCreation implements Serializable {

	/**
	 * 创作ID
	 */
	private String creationId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 提示词
	 */
	private String prompt;

	/**
	 * 歌词
	 */
	private String lyrics;

	/**
	 * 模型
	 */
	private String model;

	/**
	 * 音乐类型 0:音乐 1:纯音乐
	 */
	private Integer musicType;

	/**
	 * 模式 0:简单模式 1:专家模式
	 */
	private Integer modeType;

	/**
	 * 设置信息
	 */
	private String settings;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

}
