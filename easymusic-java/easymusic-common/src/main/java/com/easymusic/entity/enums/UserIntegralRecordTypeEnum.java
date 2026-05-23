package com.easymusic.entity.enums;


import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;
@Getter
public enum UserIntegralRecordTypeEnum {

    CREATE_MUSIC_BACK(0, "创作音乐失败退回"),
    CREATE_MUSIC(1, "创作音乐"),
    RECHARGE(2, "充值"),
    ADMIN_ADD(3, "管理员赠送"),
    ADMIN_DEDUCT(4, "管理员扣减");


    private Integer type;
    private String desc;

    UserIntegralRecordTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static UserIntegralRecordTypeEnum getByType(Integer type) {
        Optional<UserIntegralRecordTypeEnum> recordTypeEnum =
                Arrays.stream(UserIntegralRecordTypeEnum.values())
                        .filter(value -> value.getType().equals(type)).findFirst();
        return recordTypeEnum == null ? null : recordTypeEnum.get();
    }
}
