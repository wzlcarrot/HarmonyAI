package com.easymusic.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 音乐操作
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicInfoAction implements Serializable {


	/**
	 * 操作ID
	 */
	private Integer actionId;

	/**
	 * 音乐ID
	 */
	private String musicId;

	/**
	 * 音乐用户ID
	 */
	private String musicUserId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 操作类型1:点赞
	 */
	private Integer actionType;

}
