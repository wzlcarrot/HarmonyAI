package com.easymusic.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PayInfoDTO{
    private String orderId;
    private String payUrl;
    private Boolean hasValidPendingOrder; // 是否存在有效期内待付款订单
    private String existingOrderId; // 已存在的待付款订单ID
    private Long expireTime; // 订单过期时间戳（毫秒）
    private BigDecimal amount; // 订单金额
    private String productName; // 商品名称
    private Integer integral; // 积分数量

}
