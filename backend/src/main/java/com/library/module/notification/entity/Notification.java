package com.library.module.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内信：只发给某一个读者，与「公告」（所有人可见）区分开。
 *
 * <p>三类来源见 {@link com.library.module.notification.NotificationType}：
 * 到期前提醒、到书通知、预约过期。每条都带 {@code dedupeKey}，
 * 由唯一索引兜底实现「定时任务重复执行不重复发信」。
 */
@Data
@TableName("notification")
public class Notification implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 见 NotificationType */
    private String type;

    private String title;

    private String content;

    /** 关联业务 ID：DUE_SOON → 借阅记录 ID；RESERVATION_* → 预约 ID */
    private Long relatedId;

    /** 幂等键，唯一索引；为 null 时不参与唯一性（人工群发可用） */
    private String dedupeKey;

    /** 0 未读 / 1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
