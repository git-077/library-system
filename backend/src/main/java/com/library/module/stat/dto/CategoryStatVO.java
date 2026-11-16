package com.library.module.stat.dto;

import lombok.Data;

/**
 * 分类维度馆藏统计（饼图数据源）。
 */
@Data
public class CategoryStatVO {

    private Long categoryId;

    private String categoryName;

    /** 该分类在册书目品种数 */
    private int bookCount;

    /** 该分类在馆可借册数 */
    private int stock;

    /** 该分类已借出册数 */
    private int borrowed;
}
