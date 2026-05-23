package com.easymusic.entity.query;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 系统字典参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SysDictQuery extends BaseParam {


	/**
	 * 字典ID
	 */
	private Integer dictId;

	/**
	 * 字典编号
	 */
	private String dictCode;

	private String dictCodeFuzzy;

	/**
	 * 父级字典ID
	 */
	private String dictPcode;

	private String dictPcodeFuzzy;

	/**
	 * 字典值
	 */
	private String dictValue;

	private String dictValueFuzzy;

	/**
	 * 字典描述
	 */
	private String dictDesc;

	private String dictDescFuzzy;

	/**
	 * 排序号
	 */
	private Integer sort;

	public SysDictQuery(String dictCode) {

	}
}
