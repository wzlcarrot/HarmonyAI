package com.easymusic.entity.enums;


import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
@Getter
public enum PayOrderTypeEnum {

    PAY_CODE(0, "", "付款码支付"),
    PAY_ALIPAY(1, "payChannel4Alipay", "支付宝支付");


    private Integer type;
    private String beanName;
    private String desc;

    PayOrderTypeEnum(Integer type, String beanName, String desc) {
        this.type = type;
        this.beanName = beanName;
        this.desc = desc;
    }

    public static PayOrderTypeEnum getByType(Integer type) {
        Optional<PayOrderTypeEnum> typeEnum = Arrays.stream(PayOrderTypeEnum.values()).filter(value -> value.getType().equals(type)).findFirst();
        return typeEnum == null ? null : typeEnum.get();
    }
}