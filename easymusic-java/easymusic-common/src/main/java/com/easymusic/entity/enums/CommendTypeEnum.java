package com.easymusic.entity.enums;

import lombok.Getter;

@Getter
public enum CommendTypeEnum {

    NOT_COMMEND(0, "不推荐"),
    COMMEND(1, "推荐");

    private Integer type;
    private String desc;

    CommendTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
