package com.easymusic.entity.dto;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class MusicLyricsDTO {
    private BigDecimal start;
    private BigDecimal end;
    private String text;

}
