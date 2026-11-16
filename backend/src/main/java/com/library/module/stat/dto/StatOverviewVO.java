package com.library.module.stat.dto;

import lombok.Data;

/**
 * 管理后台统计概览出参（仅管理员可见）。
 */
@Data
public class StatOverviewVO {

    private long userCount;

    private long bookCount;

    private long categoryCount;

    private long noticeCount;

    /** 当前在借记录数 */
    private long activeBorrowCount;

    /** 逾期未还记录数 */
    private long overdueCount;

    /** 全馆藏书总量（在馆 + 已借出） */
    private long totalStock;
}
