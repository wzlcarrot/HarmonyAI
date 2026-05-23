package com.easymusic.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * 音乐创作信息参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicCreationQuery extends BaseParam {
	/**
	 * 创作ID
	 */
	private String creationId;

	private String creationIdFuzzy;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 提示词
	 */
	private String prompt;

	private String promptFuzzy;

	/**
	 * 歌词
	 */
	private String lyrics;

	private String lyricsFuzzy;

	/**
	 * 模型
	 */
	private String model;

	private String modelFuzzy;

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

	private String settingsFuzzy;

	/**
	 * 创建时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

}
