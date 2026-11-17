-- ============================================================================
-- V3：预约排队 + 站内信
--
-- 背景：热门书借完后读者只能反复刷页面看有没有还回来，「到馆了没人知道」，
-- 库存长期躺在架子上；同时借阅到期只能靠读者自己记，逾期后才被公告点名。
--
-- 设计要点：
-- 1) book.held 记录「已被某个待取书预约锁定」的副本数，于是库存口径固定为：
--        stock（可借）+ held（待取书锁定）+ borrowed（已借出）= 馆藏总量
--    读者借书时只认 stock，所以被预约锁定的副本不会被别人抢走。
-- 2) reservation.active_flag 是「唯一约束的偏门用法」：MySQL 没有部分索引，
--    把有效预约置 1、终态（取消/过期/已借出）置 NULL，配合
--    UNIQUE KEY (user_id, book_id, active_flag) 实现「一人一书同时只能有一条有效预约」，
--    同时允许历史上留多条已结束记录（NULL 不参与唯一性比较）。
-- 3) notification.dedupe_key 唯一索引承担「定时任务幂等」：同一条借阅在同一应还日期
--    只提醒一次，任务重跑或两台机器同时跑都不会重复发信。
-- ============================================================================

ALTER TABLE `book`
    ADD COLUMN `held` INT NOT NULL DEFAULT 0 COMMENT '已被到书预约锁定的副本数（不计入可借库存）'
        AFTER `borrowed`;

CREATE TABLE IF NOT EXISTS `reservation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '预约人ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0排队中 1待取书 2已取消 3已过期 4已借出',
  `active_flag` TINYINT DEFAULT 1 COMMENT '有效标记：0排队中/1待取书 为 1，终态置 NULL（配合唯一索引）',
  `ready_time` DATETIME DEFAULT NULL COMMENT '到书通知时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '保留截止时间（到馆后 N 天未取则失效）',
  `finish_time` DATETIME DEFAULT NULL COMMENT '结束时间（取消 / 过期 / 借出）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  -- 一人一书只能有一条「进行中」的预约；终态记录为 NULL，可无限累积
  UNIQUE KEY `uk_active` (`user_id`, `book_id`, `active_flag`),
  -- 队列查询：where book_id=? and status=0 order by id
  KEY `idx_book_status` (`book_id`, `status`, `id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约排队表';

CREATE TABLE IF NOT EXISTS `notification` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '接收人ID',
  `type` VARCHAR(32) NOT NULL COMMENT 'DUE_SOON 到期前提醒 / RESERVATION_READY 到书通知 / RESERVATION_EXPIRED 预约过期',
  `title` VARCHAR(120) NOT NULL COMMENT '标题（列表直接展示）',
  `content` VARCHAR(500) DEFAULT NULL COMMENT '正文',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID：借阅记录ID 或 预约ID，前端据此跳转',
  `dedupe_key` VARCHAR(80) DEFAULT NULL COMMENT '幂等键：定时任务重跑不会重复发信',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_dedupe` (`dedupe_key`),
  -- 铃铛拉未读数：where user_id=? and is_read=0
  KEY `idx_user_read` (`user_id`, `is_read`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信（读者通知）表';
