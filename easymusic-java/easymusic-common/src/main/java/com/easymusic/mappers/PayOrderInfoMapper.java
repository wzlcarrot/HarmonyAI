package com.easymusic.mappers;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 支付订单信息 数据库操作接口
 */
public interface PayOrderInfoMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据OrderId更新
	 */
	 Integer updateByOrderId(@Param("bean") T t,@Param("orderId") String orderId);


	/**
	 * 根据OrderId删除
	 */
	 Integer deleteByOrderId(@Param("orderId") String orderId);


	/**
	 * 根据OrderId获取对象
	 */
	 T selectByOrderId(@Param("orderId") String orderId);

	/**
	 * 分页查询订单ID列表（用于布隆过滤器历史数据同步）
	 * @param pageNo 页码（从1开始）
	 * @param pageSize 每页大小
	 * @return 订单ID列表
	 */
	List<String> selectOrderIdsByPage(@Param("pageNo") int pageNo, @Param("pageSize") int pageSize);

}
