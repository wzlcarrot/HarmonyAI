package com.easymusic.entity.enums;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum PayCodeStatusEnum {

    NO_USE(0, "未使用"),
    USED(1, "已使用");

    private Integer status;
    private String desc;

    PayCodeStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}
