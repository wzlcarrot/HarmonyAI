package com.easymusic.controller;

import com.easymusic.entity.po.SysDict;
import com.easymusic.entity.query.SysDictQuery;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.service.SysDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController
@Validated
@Slf4j
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SysDictController extends ABaseController{

    private final SysDictService sysDictService;

    @RequestMapping("/loadSysDictList")
    public ResponseVO loadSySDictList(SysDictQuery query) {
        query.setOrderBy("sort asc");

        PaginationResultVO result = sysDictService.findListByPage(query);
        return getSuccessResponseVO(result);

    }

    @RequestMapping("/delSysDict")
    public ResponseVO delSysDict(@NotNull Integer dictId){
        sysDictService.delSysDictByDictId(dictId);   //删除指定父类和子类的字典数据
        return getSuccessResponseVO("删除字典成功");
    }


    @RequestMapping("/saveSysDict")
    public ResponseVO saveSysDict(SysDict sysDict){
        sysDictService.saveSysDict(sysDict);
        return getSuccessResponseVO("保存字典成功");
    }

    @RequestMapping("/changeSort")
    public ResponseVO changeSort(@NotEmpty String dictPcode, @NotEmpty String dictIds){
        sysDictService.changeSort(dictPcode, dictIds);
        return getSuccessResponseVO("排序成功");
    }
}
