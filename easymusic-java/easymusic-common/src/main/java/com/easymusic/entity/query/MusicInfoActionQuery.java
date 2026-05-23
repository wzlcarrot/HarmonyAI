package com.easymusic.entity.query;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音乐操作参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicInfoActionQuery extends BaseParam {


	/**
	 * 操作ID
	 */
	private Integer actionId;

	/**
	 * 音乐ID
	 */
	private String musicId;

	private String musicIdFuzzy;

	/**
	 * 音乐用户ID
	 */
	private String musicUserId;

	private String musicUserIdFuzzy;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 操作类型1:点赞
	 */
	private Integer actionType;


}
