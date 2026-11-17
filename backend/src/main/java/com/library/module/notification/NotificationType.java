package com.library.module.notification;

/**
 * 站内信类型。
 *
 * <p>用字符串而不是枚举落库：新增类型不需要改表，前端也能按字符串直接判断跳转目标；
 * 代价是类型多了容易出现拼写不一致，所以统一从这里取常量，不在业务代码里写字面量。
 */
public final class NotificationType {

    /** 到期前提醒：relatedId = borrow_record.id */
    public static final String DUE_SOON = "DUE_SOON";

    /** 到书通知：预约的副本已锁定，relatedId = reservation.id */
    public static final String RESERVATION_READY = "RESERVATION_READY";

    /** 预约过期：保留期内未取书，relatedId = reservation.id */
    public static final String RESERVATION_EXPIRED = "RESERVATION_EXPIRED";

    private NotificationType() {
    }
}
