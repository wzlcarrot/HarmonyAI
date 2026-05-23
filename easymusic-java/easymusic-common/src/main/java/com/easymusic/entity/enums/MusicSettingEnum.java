package com.easymusic.entity.enums;

import lombok.Getter;

@Getter
public enum MusicSettingEnum {
    MUSIC_GENER("musicGener", "曲风"),
    MUSIC_EMOTION("musicEmotion", "情绪"),
    MUSIC_SEX("musicSex", "人声");

    private String keyCode;
    private String typeDesc;

    MusicSettingEnum(String keyCode, String typeDesc) {
        this.keyCode = keyCode;
        this.typeDesc = typeDesc;
    }

}