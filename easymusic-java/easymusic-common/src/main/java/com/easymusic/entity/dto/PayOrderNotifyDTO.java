package com.easymusic.entity.dto;

import lombok.Data;

@Data
public class PayOrderNotifyDTO {
    /**
     * 订单号
     */
    private String orderId;
    /**
     * 支付订单号 通道订单号
     */
    private String channelOrderId;

}
