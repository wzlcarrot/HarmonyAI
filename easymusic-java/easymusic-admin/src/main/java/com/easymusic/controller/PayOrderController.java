package com.easymusic.controller;

import com.easymusic.entity.query.PayOrderInfoQuery;
import com.easymusic.entity.vo.PaginationResultVO;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.service.PayOrderInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@Slf4j
@RequiredArgsConstructor
public class PayOrderController extends ABaseController {

    private final PayOrderInfoService payOrderInfoService;

    @RequestMapping("/loadOrder")
    public ResponseVO loadOrder(PayOrderInfoQuery orderInfoQuery) {
        orderInfoQuery.setOrderBy("p.create_time desc");
        orderInfoQuery.setQueryUser(true);
        log.info("orderInfoQuery:{}",orderInfoQuery);

        if(orderInfoQuery.getOrderId()!=null){
            orderInfoQuery.setOrderIdFuzzy(orderInfoQuery.getOrderId());
            orderInfoQuery.setOrderId(null);
        }

        PaginationResultVO resultVO = payOrderInfoService.findListByPage(orderInfoQuery);
        return getSuccessResponseVO(resultVO);
    }

}
