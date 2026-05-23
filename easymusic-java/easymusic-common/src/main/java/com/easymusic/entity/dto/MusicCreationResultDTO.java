package com.easymusic.entity.dto;

import lombok.Data;

import java.util.List;
@Data
public class MusicCreationResultDTO {
    private String taskId;
    private String title;
    private Integer duration;
    private String audioUrl;
    private String audioHiUrl;
    private List<Lyrics> lyricsList;
    private Boolean createSuccess;

    @Data
    public class Lyrics {
        private double start;
        private double end;
        private String text;

    }
}