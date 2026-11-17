package com.library.module.notification.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.result.Result;
import com.library.common.security.SecurityUtil;
import com.library.module.notification.entity.Notification;
import com.library.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 站内信接口（读者 / 管理员均可，只能看自己的）。
 *
 * <p>为什么不做「发给所有人」的接口：那属于公告（{@code /api/notices}）的职责，
 * 两者的可见范围与生命周期不同，混在一起会让权限判断变得别扭。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER')")
public class NotificationController {

    private final NotificationService notificationService;

    /** 我的通知分页：unreadOnly=true 只看未读 */
    @GetMapping("/page")
    public Result<IPage<Notification>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return Result.ok(notificationService.pageMy(SecurityUtil.getCurrentUserId(), pageNum, pageSize, unreadOnly));
    }

    /** 未读数：铃铛角标轮询 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        long count = notificationService.unreadCount(SecurityUtil.getCurrentUserId());
        return Result.ok(Map.of("count", count));
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, SecurityUtil.getCurrentUserId());
        return Result.ok();
    }

    /** 全部已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtil.getCurrentUserId());
        return Result.ok();
    }
}
