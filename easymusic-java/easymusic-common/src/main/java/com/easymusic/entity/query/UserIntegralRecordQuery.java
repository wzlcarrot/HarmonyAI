package com.easymusic.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 用户积分记录信息参数
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserIntegralRecordQuery extends BaseParam {


	/**
	 * 自增ID
	 */
	private Integer recordId;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 积分
	 */
	private Integer changeIntegral;

	/**
	 * 创建时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 记录类型 0:创作失败退回 1:创作消耗 2:充值 3:系统赠送
	 */
	private Integer recordType;

	/**
	 * 业务ID
	 */
	private String businessId;

	private String businessIdFuzzy;

	/**
	 * 充值金额
	 */
	private BigDecimal amount;

}
