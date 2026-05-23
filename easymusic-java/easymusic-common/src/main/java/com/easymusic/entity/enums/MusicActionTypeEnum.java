package com.easymusic.entity.enums;

import lombok.Getter;

@Getter
public enum MusicActionTypeEnum {
    GOOD(1, "点赞");

    private Integer type;
    private String desc;

    MusicActionTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

}
