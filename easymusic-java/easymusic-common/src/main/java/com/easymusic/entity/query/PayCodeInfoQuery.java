package com.easymusic.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 支付码参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayCodeInfoQuery extends BaseParam {


	/**
	 * 支付码
	 */
	private String payCode;

	private String payCodeFuzzy;

	/**
	 * 金额
	 */
	private BigDecimal amount;

	/**
	 * 创建时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 使用用户ID
	 */
	private String useUserId;

	private String useUserIdFuzzy;

	/**
	 * 使用时间
	 */
	private String useTime;

	private String useTimeStart;

	private String useTimeEnd;

	/**
	 * 状态 0:待使用 1:已使用
	 */
	private Integer status;

	private Boolean queryUser;

}
