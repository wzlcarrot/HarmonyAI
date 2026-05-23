package com.easymusic.controller;

import com.easymusic.annotation.GlobalInterceptor;
import com.easymusic.entity.dto.PayInfoDTO;
import com.easymusic.entity.dto.TokenUserInfoDTO;

import com.easymusic.entity.enums.ProductOnSaleTypeEnum;
import com.easymusic.entity.po.ProductInfo;
import com.easymusic.entity.query.ProductInfoQuery;
import com.easymusic.entity.vo.ResponseVO;
import com.easymusic.exception.BusinessException;

import com.easymusic.redis.RedisComponent;
import com.easymusic.service.PayOrderInfoService;
import com.easymusic.service.ProductInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@Slf4j
@Validated
@RequestMapping("/buy")
@RequiredArgsConstructor
public class BuyController extends ABaseController{

    private final ProductInfoService productInfoService;

    private final PayOrderInfoService payOrderInfoService;

    private final RedisComponent redisComponent;

    @RequestMapping("/loadProduct")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO loadProduct() {

        ProductInfoQuery query = new ProductInfoQuery();
        query.setOrderBy("p.sort asc");
        query.setOnsaleType(ProductOnSaleTypeEnum.ON_SALE.getType());
        List<ProductInfo> productInfoList = productInfoService.findListByParam(query);

        return getSuccessResponseVO(productInfoList);

    }

    @RequestMapping("/getPayInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getPayInfo(@NotEmpty String productId, @NotNull Integer payType){
        if((payType==1||payType==2)==false){
            throw new BusinessException("请选择正确的支付方式");
        }
        //获取支付方式
        PayInfoDTO payInfoDTO = payOrderInfoService.getPayInfo(getTokenUserInfoDTO(),productId,payType);

        return getSuccessResponseVO(payInfoDTO);
    }

    /**
     * 检查是否有有效期内待付款订单
     */
    @RequestMapping("/checkPendingOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO checkPendingOrder(){
        PayInfoDTO payInfoDTO = payOrderInfoService.checkPendingOrder(getTokenUserInfoDTO());
        return getSuccessResponseVO(payInfoDTO);
    }

    @RequestMapping("/buyByPayCode")
    public ResponseVO buyByPayCode(@NotEmpty String checkCodeKey,
                                   @NotEmpty String checkCode,
                                   @NotEmpty String payCode,
                                   @NotEmpty String productId){

        if (!checkCode.equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
            throw new BusinessException("图片验证码不正确");
        }
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
        payOrderInfoService.buyByPayCode(productId, payCode, tokenUserInfoDTO.getUserId());

        redisComponent.cleanCheckCode(checkCodeKey);

        return getSuccessResponseVO("购买支付码成功");

    }

    @RequestMapping("/havePay")
    @GlobalInterceptor(checkLogin = true)
    private ResponseVO havePay(@NotEmpty String orderId){
        TokenUserInfoDTO tokenUserInfoDTO = getTokenUserInfoDTO();
        Integer integral = payOrderInfoService.havePay(orderId, tokenUserInfoDTO.getUserId());

        tokenUserInfoDTO.setIntegral(integral);
        return getSuccessResponseVO(tokenUserInfoDTO);
    }
    
    /**
     * 检查支付订单状态
     * @param orderId
     * @return
     */
    @RequestMapping("/checkPayOrder")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO checkPayOrder(@NotEmpty String orderId) {
        try {

            boolean isPaid = payOrderInfoService.checkPayOrder(orderId);
            log.info("检查订单成功");
            return getSuccessResponseVO(isPaid);
        } catch (Exception e) {
            log.error("检查支付订单失败",e);
            return getSuccessResponseVO(false);
        }
    }
}