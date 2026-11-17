package com.library.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.module.notification.entity.Notification;
import com.library.module.notification.mapper.NotificationMapper;
import com.library.module.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 站内信实现。
 *
 * <p>幂等靠数据库唯一索引 {@code uk_dedupe} 而不是「先查再插」：
 * 定时任务在集群里可能两台机器同时跑同一分钟，先查再插存在竞态窗口，
 * 唯一索引是唯一可靠的判重手段。捕获 {@link DuplicateKeyException} 视为「已发过」，静默返回。
 *
 * <p>这里刻意不声明 {@code @Transactional}：单条 insert 本身就是原子的。
 * 若被调用方的事务包着（例如还书事务里发到书通知），冲突异常在本地捕获即可，
 * MySQL 不会因此作废整个事务；反过来如果在这里开一个新事务，
 * 反而会出现「业务已回滚、通知却已落库」的脏数据。
 */
@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
        implements NotificationService {

    @Override
    public boolean send(Long userId, String type, String title, String content,
                        Long relatedId, String dedupeKey) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setDedupeKey(dedupeKey);
        n.setIsRead(0);
        n.setCreateTime(LocalDateTime.now());
        try {
            return save(n);
        } catch (DuplicateKeyException e) {
            // 幂等命中：同一条提醒已经发过，属于正常路径，不打堆栈
            log.debug("站内信命中幂等键，跳过投递：{}", dedupeKey);
            return false;
        }
    }

    @Override
    public IPage<Notification> pageMy(Long userId, long pageNum, long pageSize, boolean unreadOnly) {
        return page(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(unreadOnly, Notification::getIsRead, 0)
                .orderByDesc(Notification::getId));
    }

    @Override
    public long unreadCount(Long userId) {
        return lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .count();
    }

    @Override
    public void markRead(Long id, Long userId) {
        // 带上 userId 条件：避免猜 id 把别人的消息标成已读
        update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }

    @Override
    public void markAllRead(Long userId) {
        update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }
}
