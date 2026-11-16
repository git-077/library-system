package com.library.module.stat.dto;

import lombok.Data;

/**
 * 单日借还趋势点（折线图数据源）。
 */
@Data
public class BorrowTrendVO {

    /** 日期，yyyy-MM-dd */
    private String date;

    /** 当日借出册数 */
    private int borrowCount;

    /** 当日归还册数 */
    private int returnCount;
}
