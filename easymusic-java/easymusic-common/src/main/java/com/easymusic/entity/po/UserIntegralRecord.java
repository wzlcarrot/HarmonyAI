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
 * 用户积分记录信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserIntegralRecord implements Serializable {


	/**
	 * 自增ID
	 */
	private Integer recordId;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 积分
	 */
	private Integer changeIntegral;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 记录类型 0:创作失败退回 1:创作消耗 2:充值 3:系统赠送
	 */
	private Integer recordType;

	/**
	 * 业务ID
	 */
	private String businessId;

	/**
	 * 充值金额
	 */
	private BigDecimal amount;

}
