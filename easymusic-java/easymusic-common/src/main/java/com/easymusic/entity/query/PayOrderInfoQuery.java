package com.easymusic.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 支付订单信息参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayOrderInfoQuery extends BaseParam {

	/**
	 * 支付订单
	 */
	private String orderId;

	private String orderIdFuzzy;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 商品ID
	 */
	private String productId;

	private String productIdFuzzy;

	/**
	 * 商品名称
	 */
	private String productName;

	private String productNameFuzzy;

	/**
	 * 金额
	 */
	private BigDecimal amount;

	/**
	 * 支付通道订单ID
	 */
	private String channelOrderId;

	private String channelOrderIdFuzzy;

	/**
	 * 订单创建时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 支付时间
	 */
	private String payTime;

	private String payTimeStart;

	private String payTimeEnd;

	/**
	 * 0:待支付 1:支付完成
	 */
	private Integer status;

	/**
	 * 购买积分
	 */
	private Integer integral;

	/**
	 * 支付信息
	 */
	private String payInfo;

	private String payInfoFuzzy;

	/**
	 * 支付类型
	 */
	private Integer payType;

	private Boolean queryUser;

	private String nickNameFuzzy;

}
