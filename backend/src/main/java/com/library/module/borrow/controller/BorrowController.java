package com.library.module.borrow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.common.util.CsvUtil;
import com.library.module.borrow.dto.OverdueCheckVO;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.service.BorrowService;
import com.library.common.security.LoginUser;
import com.library.common.security.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 借阅接口
 * 借书 / 还书 / 续借 / 我的记录：管理员和读者均可；
 * 全量记录分页：仅管理员
 */
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    /** 到期前提醒的默认窗口（天），手动触发时不传参就用它 */
    @Value("${library.borrow.due-soon-days:3}")
    private int dueSoonDays;

    /** 发起借阅 */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Audit(module = "借阅", action = "借出")
    public Result<Void> borrow(@RequestParam Long bookId) {
        borrowService.borrowBook(SecurityUtil.getCurrentUserId(), bookId);
        return Result.ok();
    }

    /** 归还 */
    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Audit(module = "借阅", action = "归还")
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id, SecurityUtil.getLoginUser());
        return Result.ok();
    }

    /** 续借 */
    @PutMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Audit(module = "借阅", action = "续借")
    public Result<Void> renew(@PathVariable Long id) {
        borrowService.renewBook(id, SecurityUtil.getLoginUser());
        return Result.ok();
    }

    /** 我的借阅历史 */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Result<IPage<BorrowRecord>> my(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Integer status) {
        return Result.ok(borrowService.pageMy(SecurityUtil.getCurrentUserId(), pageNum, pageSize, status));
    }

    /** 全部借阅记录（管理员） */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<BorrowRecord>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(borrowService.pageAll(pageNum, pageSize, status));
    }

    /**
     * 手动触发逾期扫描（定时任务之外的补救入口，幂等）。
     * 用于演示或定时任务漏跑后的即时修正。
     */
    @PostMapping("/overdue-check")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "借阅", action = "逾期扫描")
    public Result<OverdueCheckVO> overdueCheck() {
        return Result.ok(borrowService.checkOverdue());
    }

    /**
     * 手动触发到期前提醒（管理员，幂等）。
     *
     * <p>和定时任务共用同一段逻辑：同一条借阅在同一应还日期只发一次，
     * 所以补跑或连点都不会重复骚扰读者。
     *
     * @param aheadDays 提前多少天，默认取配置里的值（3 天）
     */
    @PostMapping("/due-soon-remind")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "借阅", action = "到期提醒")
    public Result<Map<String, Object>> dueSoonRemind(
            @RequestParam(required = false) Integer aheadDays) {
        int days = aheadDays == null || aheadDays <= 0 ? dueSoonDays : aheadDays;
        int sent = borrowService.sendDueSoonReminders(days);
        return Result.ok(Map.of("aheadDays", days, "sent", sent));
    }

    /**
     * 导出借阅记录 CSV（仅管理员）。
     *
     * <p>直接写响应流而不是先落盘：数据量再大也只是流式输出，
     * 不需要为临时文件管理磁盘与清理策略。
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "借阅", action = "导出CSV", saveParams = false)
    public void export(@RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        List<BorrowRecord> records = borrowService.listForExport(status);
        List<List<?>> rows = new ArrayList<>(records.size());
        LocalDateTime now = LocalDateTime.now();
        for (BorrowRecord r : records) {
            // 逾期天数只对在借记录有意义；已归还的显示归还时间即可
            String overdueDays = "";
            if (r.getStatus() != null && r.getStatus() == 0 && r.getDueDate() != null
                    && r.getDueDate().isBefore(now)) {
                overdueDays = String.valueOf(
                        Math.max(1, ChronoUnit.DAYS.between(r.getDueDate().toLocalDate(), now.toLocalDate())));
            }
            rows.add(Arrays.asList(
                    r.getId(),
                    r.getUsername(),
                    r.getBookName(),
                    format(r.getBorrowDate()),
                    format(r.getDueDate()),
                    format(r.getReturnDate()),
                    r.getStatus() != null && r.getStatus() == 1 ? "已归还" : "借出中",
                    overdueDays,
                    r.getRenewCount()
            ));
        }
        CsvUtil.write(response, "borrows", CSV_HEADER, rows);
    }

    private static final List<String> CSV_HEADER = List.of(
            "记录ID", "借阅人", "书名", "借出时间", "应还时间", "归还时间", "状态", "逾期天数", "续借次数");

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(CSV_TIME);
    }
}
