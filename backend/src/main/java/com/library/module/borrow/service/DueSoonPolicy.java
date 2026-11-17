package com.library.module.borrow.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 到期前提醒的判定规则（纯函数，无 Spring 依赖，便于单测）。
 *
 * <p>抽出来的原因：提醒窗口的边界很容易写错一格。
 * 「还剩 3 天」到底包含第 3 天当天？已逾期的书要不要再提醒一次？
 * 这类问题放在 Service 里只能靠手工点页面验证，抽成纯函数就能用单元测试钉死。
 */
public final class DueSoonPolicy {

    private DueSoonPolicy() {
    }

    /**
     * 是否落在提醒窗口内：{@code now <= due <= now + aheadDays}。
     *
     * <p>两端都闭：刚好今天到期（remainDays = 0）要提醒；
     * 刚好第 {@code aheadDays} 天到期也要提醒，否则读者永远收不到「还有 3 天」这条。
     *
     * <p>已逾期的（{@code due < now}）<b>不在</b>窗口内：那是催还公告的职责，
     * 两边都发会让读者同一天收到两条重复消息。
     */
    public static boolean inWindow(LocalDateTime due, LocalDateTime now, int aheadDays) {
        if (due == null) {
            return false;
        }
        return !due.isBefore(now) && !due.isAfter(now.plusDays(aheadDays));
    }

    /** 剩余天数：按日历天算（今天到期返回 0，明天到期返回 1） */
    public static long remainDays(LocalDateTime due, LocalDateTime now) {
        return ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate());
    }

    /**
     * 幂等键 {@code DUE_SOON:借阅ID:应还日期}。
     *
     * <p>为什么要带日期：读者续借后应还日期变了，理应再提醒一次；
     * 而同一个应还日期绝不能每天提醒一遍，否则一本快到期的书连发 3 条就成了骚扰。
     */
    public static String dedupeKey(Long recordId, LocalDateTime due) {
        return "DUE_SOON:" + recordId + ":" + due.toLocalDate();
    }

    /** 标题：今天到期与还剩 N 天用不同措辞，后者带数字更能促动读者 */
    public static String title(String bookName, long remainDays) {
        return remainDays <= 0
                ? "今天到期：《" + bookName + "》"
                : "还有 " + remainDays + " 天到期：《" + bookName + "》";
    }
}
