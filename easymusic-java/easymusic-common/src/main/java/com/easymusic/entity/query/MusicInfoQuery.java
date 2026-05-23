package com.easymusic.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;


/**
 * 音乐信息参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicInfoQuery extends BaseParam {

	/**
	 * 音乐ID
	 */
	private String musicId;

	private String musicIdFuzzy;

	/**
	 * 用户ID
	 */
	private String userId;   //创建音乐的用户ID

	private String userIdFuzzy;

	/**
	 * 任务ID
	 */
	private String taskId;

	private String taskIdFuzzy;

	/**
	 * 创作ID
	 */
	private String creationId;

	private String creationIdFuzzy;

	/**
	 * 标题
	 */
	private String musicTitle;

	private String musicTitleFuzzy;

	/**
	 * 封面
	 */
	private String cover;

	private String coverFuzzy;

	/**
	 * 音乐地址
	 */
	private String audioPath;

	private String audioPathFuzzy;

	/**
	 * 持续时间
	 */
	private Integer duration;

	/**
	 * 歌词
	 */
	private String lyrics;

	private String lyricsFuzzy;

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
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 0:生成音乐中 1:生成完毕
	 */
	private Integer musicStatus;

	/**
	 * 音乐类型 0:音乐 1:纯音乐
	 */
	private Integer musicType;

	private Boolean queryUser;

	private String currentUserId;

	private Boolean queryLikeMusic;

	private String likeUserId;   //喜欢此音乐的用户ID

	private List<String> musicIdList;

}
