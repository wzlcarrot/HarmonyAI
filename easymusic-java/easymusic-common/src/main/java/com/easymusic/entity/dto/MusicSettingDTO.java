package com.easymusic.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MusicSettingDTO {
    private String musicGener;
    private String musicEmotion;
    private String musicSex;

}
