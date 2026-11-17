package com.library.module.reservation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.common.security.LoginUser;
import com.library.common.security.SecurityUtil;
import com.library.module.reservation.dto.BookReservationVO;
import com.library.module.reservation.entity.Reservation;
import com.library.module.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 预约排队接口。
 *
 * <p>读者：预约 / 取消 / 我的预约 / 单书预约态；
 * 管理员：额外的全量分页（看每一本的排队情况），取消动作与读者共用（可代读者取消）。
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class ReservationController {

    private final ReservationService reservationService;

    /** 预约一本已借完的书 */
    @PostMapping
    @Audit(module = "预约", action = "预约")
    public Result<Long> reserve(@RequestParam Long bookId) {
        return Result.ok(reservationService.reserve(SecurityUtil.getCurrentUserId(), bookId));
    }

    /** 取消预约（本人或管理员） */
    @DeleteMapping("/{id}")
    @Audit(module = "预约", action = "取消")
    public Result<Void> cancel(@PathVariable Long id) {
        LoginUser operator = SecurityUtil.getLoginUser();
        reservationService.cancel(id, operator);
        return Result.ok();
    }

    /** 我的预约（含排队位置） */
    @GetMapping("/my")
    public Result<List<Reservation>> my() {
        return Result.ok(reservationService.myList(SecurityUtil.getCurrentUserId()));
    }

    /** 单本书的预约态：队列长度 + 我的状态，供详情页按钮使用 */
    @GetMapping("/book/{bookId}")
    public Result<BookReservationVO> bookStatus(@PathVariable Long bookId) {
        return Result.ok(reservationService.bookStatus(SecurityUtil.getCurrentUserId(), bookId));
    }

    /** 管理员：预约队列分页 */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<Reservation>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(reservationService.pageAdmin(pageNum, pageSize, status, keyword));
    }
}
