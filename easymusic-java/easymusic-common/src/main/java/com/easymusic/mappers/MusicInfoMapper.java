package com.easymusic.mappers;

import com.easymusic.entity.po.MusicInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 音乐信息 数据库操作接口
 */
public interface MusicInfoMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据MusicId更新
	 */
	 Integer updateByMusicId(@Param("bean") T t,@Param("musicId") String musicId);


	/**
	 * 根据MusicId删除
	 */
	 Integer deleteByMusicId(@Param("musicId") String musicId);


	/**
	 * 根据MusicId获取对象
	 */
	 MusicInfo selectByMusicId(@Param("musicId") String musicId);


	/**
	 * 根据TaskId更新
	 */
	 Integer updateByTaskId(@Param("bean") T t,@Param("taskId") String taskId);


	/**
	 * 根据TaskId删除
	 */
	 Integer deleteByTaskId(@Param("taskId") String taskId);


	/**
	 * 根据TaskId获取对象
	 */
	 MusicInfo selectByTaskId(@Param("taskId") String taskId);

	void updateMusicCount(String musicId);

	/**
	 * 更新点赞数
	 * @param musicId 音乐ID
	 * @param delta 增量，正数表示增加，负数表示减少
	 */
	void updateGoodCount(@Param("musicId") String musicId, @Param("delta") Integer delta);

	/**
	 * 分页查询音乐ID列表（用于布隆过滤器历史数据同步）
	 * @param pageNo 页码（从1开始）
	 * @param pageSize 每页大小
	 * @return 音乐ID列表
	 */
	List<String> selectMusicIdsByPage(@Param("pageNo") int pageNo, @Param("pageSize") int pageSize);

}
