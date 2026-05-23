package com.easymusic.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 商品信息参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductInfoQuery extends BaseParam {


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
	 * 封面
	 */
	private String cover;

	private String coverFuzzy;

	/**
	 * 价格
	 */
	private BigDecimal price;

	/**
	 * 商品描述
	 */
	private String productDescription;

	private String productDescriptionFuzzy;

	/**
	 * 创建时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

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
