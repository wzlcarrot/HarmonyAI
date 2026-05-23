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
 * 支付码
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayCodeInfo implements Serializable {


	/**
	 * 支付码
	 */
	private String payCode;

	/**
	 * 金额
	 */
	private BigDecimal amount;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 使用用户ID
	 */
	private String useUserId;

	/**
	 * 使用时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date useTime;

	/**
	 * 状态 0:待使用 1:已使用
	 */
	private Integer status;

	private String nickName;

}
