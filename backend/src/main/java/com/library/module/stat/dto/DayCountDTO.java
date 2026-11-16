package com.library.module.stat.dto;

import lombok.Data;

/**
 * 「日期 + 计数」中间投影，仅用于统计 SQL 与 Java 侧做补零对齐，不对外输出。
 */
@Data
public class DayCountDTO {

    /** yyyy-MM-dd */
    private String day;

    private int cnt;
}
