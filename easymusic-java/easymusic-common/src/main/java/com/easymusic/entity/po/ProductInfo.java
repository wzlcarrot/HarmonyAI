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

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;


/**
 * 商品信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductInfo implements Serializable {


	/**
	 * 商品ID
	 */
	private String productId;

	/**
	 * 商品名称
	 */
	private String productName;

	/**
	 * 封面
	 */
	private String cover;

	/**
	 * 价格
	 */
	private BigDecimal price;

	/**
	 * 商品描述
	 */
	private String productDescription;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 上架类型
	 */
	private Integer onsaleType;

	/**
	 * 购买积分
	 */
	private Integer integral;

	/**
	 * 排序号
	 */
	private Integer sort;

}
