package com.library.module.borrow.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 到期前提醒的窗口判定（纯函数，毫秒级）。
 *
 * <p>这几条边界最容易改错：差一格就会「永远不提醒」或「每天提醒一遍」，
 * 而两种错误在页面上都很难靠肉眼发现，所以用测试钉死。
 */
@DisplayName("到期前提醒：窗口边界与幂等键")
class DueSoonPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 10, 0);
    private static final int AHEAD = 3;

    @Test
    @DisplayName("今天到期在窗口内，剩余 0 天，标题用「今天到期」")
    void todayIsInWindow() {
        LocalDateTime due = NOW.withHour(18);
        assertThat(DueSoonPolicy.inWindow(due, NOW, AHEAD)).isTrue();
        assertThat(DueSoonPolicy.remainDays(due, NOW)).isZero();
        assertThat(DueSoonPolicy.title("活着", 0)).isEqualTo("今天到期：《活着》");
    }

    @Test
    @DisplayName("第 N 天（窗口右边界）仍要提醒，否则读者收不到「还有 3 天」")
    void exactlyAheadDaysIsInWindow() {
        LocalDateTime due = NOW.plusDays(AHEAD);
        assertThat(DueSoonPolicy.inWindow(due, NOW, AHEAD)).isTrue();
        assertThat(DueSoonPolicy.remainDays(due, NOW)).isEqualTo(3);
        assertThat(DueSoonPolicy.title("活着", 3)).isEqualTo("还有 3 天到期：《活着》");
    }

    @Test
    @DisplayName("超出窗口一天就不提醒，避免提前太多被忽略")
    void beyondWindowIsIgnored() {
        assertThat(DueSoonPolicy.inWindow(NOW.plusDays(AHEAD + 1), NOW, AHEAD)).isFalse();
    }

    @Test
    @DisplayName("右边界按时刻算：第 3 天但晚 1 秒即出窗口")
    void windowBoundaryIsByInstant() {
        assertThat(DueSoonPolicy.inWindow(NOW.plusDays(AHEAD).plusSeconds(1), NOW, AHEAD)).isFalse();
    }

    @Test
    @DisplayName("已逾期不在提醒窗口内：逾期归催还公告管，两边都发会重复")
    void overdueIsExcluded() {
        assertThat(DueSoonPolicy.inWindow(NOW.minusSeconds(1), NOW, AHEAD)).isFalse();
        assertThat(DueSoonPolicy.inWindow(NOW.minusDays(2), NOW, AHEAD)).isFalse();
    }

    @Test
    @DisplayName("应还时间为空时不提醒（防御脏数据）")
    void nullDueIsIgnored() {
        assertThat(DueSoonPolicy.inWindow(null, NOW, AHEAD)).isFalse();
    }

    @Test
    @DisplayName("幂等键：同一借阅同一应还日期恒定，续借后可再提醒一次")
    void dedupeKeyChangesOnlyWhenDueDateChanges() {
        LocalDateTime due = NOW.plusDays(3);
        String first = DueSoonPolicy.dedupeKey(88L, due);
        String again = DueSoonPolicy.dedupeKey(88L, due.withHour(9));

        // 同一天的不同时刻算同一次提醒：定时任务重跑不会重复发信
        assertThat(first).isEqualTo(again).isEqualTo("DUE_SOON:88:2026-03-13");
        // 续借后应还日期变了，键随之变化，读者才会收到新的一条
        assertThat(DueSoonPolicy.dedupeKey(88L, due.plusDays(30)))
                .isEqualTo("DUE_SOON:88:2026-04-12")
                .isNotEqualTo(first);
    }
}
