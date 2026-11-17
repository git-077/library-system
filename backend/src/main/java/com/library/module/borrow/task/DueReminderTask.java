package com.library.module.borrow.task;

import com.library.common.log.TraceContext;
import com.library.module.borrow.service.BorrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 到期前提醒（每日）。
 *
 * <p>和逾期扫描的分工：逾期扫描是「事后催」，会对全馆发公告；
 * 这里只在到期前 {@code aheadDays} 天给本人发一条站内信，属于「事前提醒」。
 * 两者窗口不重叠（这里只取 {@code due_date >= now}），所以一本不会被同时催和提醒。
 *
 * <p>幂等由 {@code notification.dedupe_key = DUE_SOON:借阅ID:应还日期} 保证，
 * 手滑点两次或补跑昨天的任务都不会重复发信。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DueReminderTask {

    private final BorrowService borrowService;

    /** 提前多少天提醒。3 天足够读者安排时间，又不至于太早被忽略 */
    @Value("${library.borrow.due-soon-days:3}")
    private int aheadDays;

    @Scheduled(cron = "${library.borrow.due-soon-cron:0 0 9 * * ?}")
    public void remind() {
        TraceContext.beginNew();
        try {
            int sent = borrowService.sendDueSoonReminders(aheadDays);
            log.info("到期前提醒完成：{} 天内到期，新增站内信 {} 条", aheadDays, sent);
        } catch (Exception e) {
            // 提醒失败不该影响任何业务数据，记日志等下一次执行
            log.error("到期前提醒失败，本次跳过", e);
        } finally {
            TraceContext.clear();
        }
    }
}
