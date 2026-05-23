package com.easymusic.controller;
import com.easymusic.entity.constants.Constants;
import com.easymusic.entity.enums.PayCodeStatusEnum;
import com.easymusic.entity.po.PayCodeInfo;
import com.easymusic.entity.query.PayCodeInfoQuery;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.service.PayCodeInfoService;
import com.easymusic.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/payCode")
@Slf4j
@Validated
@RequiredArgsConstructor
public class PayCodeInfoController extends ABaseController{

    private final PayCodeInfoService payCodeInfoService;

    @RequestMapping("/loadPayCodeList")
    public ResponseVO loadPayCodeList(PayCodeInfoQuery payCodeInfoQuery) {
        //其实createTimeStart和createTimeEnd都在query中
        log.info("查询开始时间：{}", payCodeInfoQuery.getCreateTimeStart());
        log.info("查询结束时间：{}", payCodeInfoQuery.getCreateTimeEnd());

        payCodeInfoQuery.setOrderBy("create_time desc");
        payCodeInfoQuery.setQueryUser(true);

        PaginationResultVO<PayCodeInfo> list = payCodeInfoService.findListByPage(payCodeInfoQuery);

        return getSuccessResponseVO(list);
    }


    @RequestMapping("/createCode")
    public ResponseVO createCode(@NotNull BigDecimal amount) {

        PayCodeInfo bean = new PayCodeInfo();
        bean.setAmount(amount);
        bean.setPayCode(StringTools.getRandomNumber(Constants.LENGTH_8));
        bean.setCreateTime(new Date());
        bean.setStatus(PayCodeStatusEnum.NO_USE.getStatus());

        payCodeInfoService.add(bean);
        return getSuccessResponseVO("创建付款码成功");
    }

    @RequestMapping("/delCode")
    public ResponseVO delCode(@NotEmpty String payCode){
        payCodeInfoService.deletePayCodeInfoByPayCode(payCode);

        return getSuccessResponseVO("删除成功");
    }

}
