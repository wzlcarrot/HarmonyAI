package com.easymusic.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
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
 * 支付订单信息
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PayOrderInfo implements Serializable {


	/**
	 * 支付了行
	 */
	private String orderId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 商品ID
	 */
	private String productId;

	/**
	 * 商品名称
	 */
	private String productName;

	/**
	 * 金额
	 */
	private BigDecimal amount;

	/**
	 * 支付通道订单ID
	 */
	private String channelOrderId;

	/**
	 * 订单创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 支付时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date payTime;

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

	/**
	 * 支付类型
	 */
	private Integer payType;

	private String nickName;

	private String nickNameFuzzy;
}
