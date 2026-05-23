package com.easymusic.entity.enums;

import lombok.Getter;

@Getter
public enum MusicStatusEnum {
    CREATING(0, "音乐生成中"),
    CREATED(1, "音乐生成完成"),
    CREATE_FAIL(2, "音乐生成失败");

    private Integer status;
    private String desc;

    MusicStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }
}