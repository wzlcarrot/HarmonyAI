package com.easymusic.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 系统字典
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SysDict implements Serializable {

	/**
	 * 字典ID
	 */
	private Integer dictId;

	/**
	 * 字典编号
	 */
	private String dictCode;

	/**
	 * 父级字典ID
	 */
	private String dictPcode;

	/**
	 * 字典值
	 */
	private String dictValue;

	/**
	 * 字典描述
	 */
	private String dictDesc;

	/**
	 * 排序号
	 */
	private Integer sort;

}
