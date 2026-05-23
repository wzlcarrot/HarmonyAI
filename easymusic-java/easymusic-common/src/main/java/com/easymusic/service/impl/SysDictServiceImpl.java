package com.easymusic.service.impl;

import java.util.*;


import com.easymusic.entity.constants.Constants;
import com.easymusic.exception.BusinessException;
import com.easymusic.redis.RedisComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.easymusic.entity.enums.PageSize;
import com.easymusic.entity.query.SysDictQuery;
import com.easymusic.entity.po.SysDict;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.query.SimplePage;
import com.easymusic.mappers.SysDictMapper;
import com.easymusic.service.SysDictService;
import com.easymusic.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


/**
 * 系统字典 业务接口实现
 */
@Service("sysDictService")
@RequiredArgsConstructor
@Slf4j
public class SysDictServiceImpl implements SysDictService {

	private final SysDictMapper<SysDict, SysDictQuery> sysDictMapper;

	private final RedisComponent redisComponent;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<SysDict> findListByParam(SysDictQuery param) {
		return this.sysDictMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(SysDictQuery param) {
		return this.sysDictMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<SysDict> findListByPage(SysDictQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<SysDict> list = this.findListByParam(param);
		PaginationResultVO<SysDict> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(SysDict bean) {
		return this.sysDictMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<SysDict> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.sysDictMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<SysDict> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.sysDictMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(SysDict bean, SysDictQuery param) {
		StringTools.checkParam(param);
		return this.sysDictMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(SysDictQuery param) {
		StringTools.checkParam(param);
		return this.sysDictMapper.deleteByParam(param);
	}

	/**
	 * 根据DictId获取对象
	 */
	@Override
	public SysDict getSysDictByDictId(Integer dictId) {
		return this.sysDictMapper.selectByDictId(dictId);
	}

	/**
	 * 根据DictId修改
	 */
	@Override
	public Integer updateSysDictByDictId(SysDict bean, Integer dictId) {
		return this.sysDictMapper.updateByDictId(bean, dictId);
	}

	/**
	 * 根据DictId删除
	 */
	@Override
	public Integer deleteSysDictByDictId(Integer dictId) {
		return this.sysDictMapper.deleteByDictId(dictId);
	}


	@Transactional(rollbackFor = Exception.class)
	public void saveSysDict(SysDict sysDict) {

		if(sysDict==null){
			throw new BusinessException("插入的字典不存在");
		}
		try{
			if (sysDict.getDictId() == null) {
				setProperSortValue(sysDict);
				this.sysDictMapper.insert(sysDict);
			} else {
				//如果sysDict存在，则进行修改
				this.sysDictMapper.updateByDictId(sysDict, sysDict.getDictId());
			}

			saveDict2Redis(sysDict.getDictPcode());
			log.info("字典插入到Redis中成功");
		}catch (Exception e){
			log.info("字典插入到Redis中失败",e);
			throw e;
		}

	}
	//总结出一个万能事务回滚成功法
	/*
	* 	try{

		}catch (Exception e){
			log.info("字典插入到Redis中失败",e);
			throw e;
		}
	*
	* */

	//为新插入的字典序设置合适的排序值
	private void setProperSortValue(SysDict sysDict) {
		// 查询同级字典项的最大sort值
		SysDictQuery query = new SysDictQuery();
		query.setDictPcode(sysDict.getDictPcode());

		List<SysDict> dict = this.sysDictMapper.selectList(query);
		log.info("dict.size():"+dict.size());
		if(dict.size()==0){
			sysDict.setSort(0);
		}
		else {
			Integer maxSort = dict.get(dict.size() - 1).getSort();
			sysDict.setSort(maxSort == null ? 1 : maxSort + 1);
		}
	}


	private void saveDict2Redis(String dictPcode) {
		if (Constants.ZERO_STR.equals(dictPcode)) {
			return;
		}

		SysDictQuery dictQuery = new SysDictQuery();
		dictQuery.setDictPcode(dictPcode);
		dictQuery.setOrderBy("sort asc");
		List<SysDict> sysDictList = this.sysDictMapper.selectList(dictQuery);
		try {
			redisComponent.saveDict(dictPcode, sysDictList);
		} catch(Exception e){
			log.error("保存字典到Redis失败",e);
			throw new BusinessException("数据缓存失败");
		}

	}


	@Override
	public void delSysDictByDictId(Integer dictId) {
		SysDict sysDict = sysDictMapper.selectByDictId(dictId);
		sysDictMapper.deleteByDictId(dictId);

		//删除子类节点
		SysDictQuery query = new SysDictQuery();
		query.setDictPcode(sysDict.getDictCode());
		sysDictMapper.deleteByParam(query);
	}

	@Override
	public void changeSort(String dictPcode, String dictIds) {

		String [] dictIdArr = dictIds.split(",");

		for(int i=0;i<dictIdArr.length;i++){
			SysDict  sysDict = new SysDict();
			sysDict.setSort(i+1);
			sysDict.setDictId(Integer.parseInt(dictIdArr[i]));
			sysDictMapper.updateByDictId(sysDict,sysDict.getDictId());
		}

	}

	public Map<String, List<SysDict>> getDictList() {
		SysDictQuery dictQuery = new SysDictQuery();
		List<SysDict> sysDictList = this.sysDictMapper.selectList(dictQuery);

		// 按 dictPcode 分组，排除 null、空值和根节点
		Map<String, List<SysDict>> result = new HashMap<>();
		for (SysDict dict : sysDictList) {
			String pcode = dict.getDictPcode();
			if (pcode != null && !pcode.isEmpty() && !Constants.ZERO_STR.equals(pcode)) {
				if (!result.containsKey(pcode)) {
					result.put(pcode, new ArrayList<>());
				}
				result.get(pcode).add(dict);
			}
		}
		// 对每个分组排序（排除根节点）
		for (Map.Entry<String, List<SysDict>> entry : result.entrySet()) {
			List<SysDict> value = entry.getValue();
			value.sort(Comparator.comparing(SysDict::getSort,
					Comparator.nullsLast(Comparator.naturalOrder())));
		}

		return result;
	}
}