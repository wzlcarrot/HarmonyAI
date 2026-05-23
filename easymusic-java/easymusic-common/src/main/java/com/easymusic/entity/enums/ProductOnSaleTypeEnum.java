package com.easymusic.entity.enums;

import lombok.Getter;

@Getter
public enum ProductOnSaleTypeEnum {
    OFF_SALE(0, "下架"), ON_SALE(1, "上架");

    private Integer type;

    private String desc;

    ProductOnSaleTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

}
