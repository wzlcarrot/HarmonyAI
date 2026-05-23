package com.easymusic.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MusicTaskDTO implements Serializable {
    private String musicId;
    private String taskId;
    private String apiCode;
    private Integer musicType;

}