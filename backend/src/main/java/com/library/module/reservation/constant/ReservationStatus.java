package com.library.module.reservation.constant;

/**
 * 预约状态机（落库为 TINYINT）。
 *
 * <pre>
 *  0 排队中 ──(有副本归还，轮到你了)──> 1 待取书 ──(借书成功)──> 4 已借出
 *     │                                  │
 *     │                                  ├──(超过保留期未取)──> 3 已过期
 *     └──────────(读者主动取消)──────────┴────────────────────> 2 已取消
 * </pre>
 *
 * <p>只有 0 和 1 是「进行中」状态，对应 {@code reservation.active_flag = 1}；
 * 终态一律把 active_flag 置 NULL，从而让唯一索引 {@code (user_id, book_id, active_flag)}
 * 允许同一本书留下多条历史记录。
 */
public final class ReservationStatus {

    /** 排队中：等副本归还 */
    public static final int WAITING = 0;

    /** 待取书：副本已锁定给他，等读者来借 */
    public static final int READY = 1;

    /** 已取消（读者主动） */
    public static final int CANCELLED = 2;

    /** 已过期（保留期内未取书） */
    public static final int EXPIRED = 3;

    /** 已借出（预约转成了借阅记录） */
    public static final int FULFILLED = 4;

    private ReservationStatus() {
    }

    /** 是否处于「进行中」：这两个状态占用 active_flag 与队列位置 */
    public static boolean isActive(int status) {
        return status == WAITING || status == READY;
    }

    /** 状态文案：前端列表直接展示，避免每个页面各写一份 switch */
    public static String text(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case WAITING -> "排队中";
            case READY -> "待取书";
            case CANCELLED -> "已取消";
            case EXPIRED -> "已过期";
            case FULFILLED -> "已借出";
            default -> "未知";
        };
    }
}
