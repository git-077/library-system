package com.library.module.borrow.dto;

import lombok.Data;

/**
 * 逾期扫描结果（定时任务与手动触发共用）。
 */
@Data
public class OverdueCheckVO {

    /** 当前逾期未还记录数（扫描后） */
    private long overdueCount;

    /** 本次新标记为逾期的记录数 */
    private int markedCount;

    /** 本次因续借/改期而恢复正常的记录数 */
    private int clearedCount;

    /** 是否新发布了催还公告 */
    private boolean noticeCreated;

    /** 公告标题（未发布时为空） */
    private String noticeTitle;
}
