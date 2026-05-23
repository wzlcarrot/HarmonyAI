package com.easymusic.entity.enums;


import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum PayOrderStatusEnum {

    NO_PAY(0, "待支付"),
    HAVE_PAY(1, "已支付");

    private Integer status;
    private String desc;

    PayOrderStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static PayOrderStatusEnum getByStatus(Integer type) {
        Optional<PayOrderStatusEnum> typeEnum = Arrays.stream(PayOrderStatusEnum.values()).filter(value -> value.getStatus().equals(type)).findFirst();
        return typeEnum == null ? null : typeEnum.get();
    }
}
