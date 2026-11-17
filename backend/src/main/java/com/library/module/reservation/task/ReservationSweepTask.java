package com.library.module.reservation.task;

import com.library.common.log.TraceContext;
import com.library.module.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 预约过期清扫。
 *
 * <p>为什么不和逾期扫描合并成一天一次：到馆保留期只有 7 天，一天扫一次意味着
 * 副本最多要被多锁近 24 小时才能轮到下一位——热门书每一小时都值钱，
 * 所以这里按小时扫（cron 可通过 library.reservation.sweep-cron 覆盖）。
 *
 * <p>任务本身幂等：状态更新带 {@code status = READY} 条件，重复执行只是空转。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationSweepTask {

    private final ReservationService reservationService;

    @Scheduled(cron = "${library.reservation.sweep-cron:0 5 * * * ?}")
    public void sweep() {
        TraceContext.beginNew();
        try {
            int handled = reservationService.sweepExpired();
            if (handled > 0) {
                log.info("预约过期清扫完成：处理 {} 条，副本已顺延给下一位", handled);
            }
        } catch (Exception e) {
            log.error("预约过期清扫失败，本次跳过（下次整点重试）", e);
        } finally {
            TraceContext.clear();
        }
    }
}
