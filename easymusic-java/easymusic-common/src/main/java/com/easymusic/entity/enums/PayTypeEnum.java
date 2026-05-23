package com.easymusic.entity.enums;

import lombok.Getter;

@Getter
public enum PayTypeEnum {
    LAOLUO("laoluo", "使用老罗封装的接口"),
    ALIPAY("alipay", "使用支付宝官方");

    private String payType;
    private String desc;

    PayTypeEnum(String payType, String desc) {
        this.payType = payType;
        this.desc = desc;
    }
}