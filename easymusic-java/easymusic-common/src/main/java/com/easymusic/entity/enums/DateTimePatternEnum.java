package com.easymusic.entity.enums;

import lombok.Data;
import lombok.Getter;

@Getter
public enum DateTimePatternEnum {
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    YYYYMMDDHHMMSS("yyyyMMddHHmmss"),
    YYYY_MM_DD("yyyy-MM-dd"),
    YYYYMMDD("yyyyMMdd");
    private String pattern;

    DateTimePatternEnum(String pattern) {
        this.pattern = pattern;
    }

}
