package com.easymusic.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUserInfoDTO {

    private String userId;
    private String nickName;
    private String account;
    private String token;
    private String avatar;
    private Integer integral;

    /**
     * 用户发布的歌曲总数
     */
    private Integer musicCount;

    /**
     * 用户所有歌曲被点赞的总数
     */
    private Integer goodCount;

}
