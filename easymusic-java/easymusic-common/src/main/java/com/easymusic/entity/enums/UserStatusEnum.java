package com.easymusic.entity.enums;
import lombok.Getter;

@Getter
public enum UserStatusEnum {
    DISABLE(0, "禁用"),
    ENABLE(1, "启用");

    UserStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    private Integer status;

    private String desc;



}
