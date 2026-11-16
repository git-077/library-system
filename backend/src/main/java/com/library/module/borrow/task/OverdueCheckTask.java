package com.library.module.borrow.task;

import com.library.module.borrow.dto.OverdueCheckVO;
import com.library.module.borrow.service.BorrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 逾期扫描定时任务。
 *
 * <p>每天凌晨 01:00 跑一次（cron 可通过配置 <code>library.overdue.cron</code> 覆盖），
 * 做两件事：刷新 borrow_record.overdue 标记；有逾期时发一条当日催还公告。
 *
 * <p>为什么用定时任务而不是每次查询时算：
 * 逾期是「随时间自然发生」的状态，读者夜里到期不会有人访问页面，
 * 靠请求触发会漏标；定时任务保证标记与公告每天准时落地，且扫描成本与访问量无关。
 *
 * <p>任务内部吞异常：单次失败不应让调度线程终止（Spring 默认会继续下次调度，
 * 但打印堆栈便于排查）。标记只是派生状态，业务判定仍以 due_date 为准，漏跑不产生业务错误。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueCheckTask {

    private final BorrowService borrowService;

    @Scheduled(cron = "${library.overdue.cron:0 0 1 * * ?}")
    public void scan() {
        try {
            OverdueCheckVO vo = borrowService.checkOverdue();
            log.info("逾期扫描完成：当前逾期 {} 条，新标记 {} 条，恢复 {} 条{}",
                    vo.getOverdueCount(), vo.getMarkedCount(), vo.getClearedCount(),
                    vo.isNoticeCreated() ? "，已发布公告《" + vo.getNoticeTitle() + "》" : "");
        } catch (Exception e) {
            log.error("逾期扫描失败，本次跳过（不影响业务判定）", e);
        }
    }
}
