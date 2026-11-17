package com.library.module.notification.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.notification.entity.Notification;

/**
 * 站内信服务。
 *
 * <p>写入接口 {@link #send} 供定时任务和其它业务模块调用：
 * 只负责「投递」，不关心业务规则；失败不应影响主流程（谁调用谁兜住异常）。
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 投递一条站内信。
     *
     * @param dedupeKey 幂等键；同一个 key 只会成功写入一次（唯一索引兜底），可为 null
     * @return true=真的写入了，false=命中幂等（已存在，跳过）
     */
    boolean send(Long userId, String type, String title, String content, Long relatedId, String dedupeKey);

    /** 我的通知分页；unreadOnly 为 true 时只返回未读 */
    IPage<Notification> pageMy(Long userId, long pageNum, long pageSize, boolean unreadOnly);

    /** 未读数：铃铛角标，60 秒轮询一次 */
    long unreadCount(Long userId);

    /** 标记单条已读（只能操作自己的） */
    void markRead(Long id, Long userId);

    /** 全部标为已读 */
    void markAllRead(Long userId);
}
