-- =============================================================
-- V1 基线结构：与 V1 发布时的表结构一致
--
-- 约定：
--   1. 本文件只做「建表」，全部使用 CREATE TABLE IF NOT EXISTS，
--      因此对已经用旧版 sql/init.sql 建过库的环境是安全的空操作；
--   2. 后续任何字段/索引改动都新增 Vn 文件，禁止再回头改这里
--     （Flyway 会校验 checksum，改历史脚本会导致启动失败）；
--   3. 字符集统一 utf8mb4；时间字段交给数据库默认值。
-- =============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin/user',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除: 0未删 1已删',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 图书分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- 图书表（软删除；stock 在馆库存 + borrowed 已借出 = 总藏书量）
CREATE TABLE IF NOT EXISTS `book` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `isbn` VARCHAR(20) NOT NULL COMMENT 'ISBN',
  `name` VARCHAR(200) NOT NULL COMMENT '书名',
  `author` VARCHAR(100) NOT NULL COMMENT '作者',
  `publisher` VARCHAR(100) DEFAULT NULL COMMENT '出版社',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `price` DECIMAL(10,2) DEFAULT NULL COMMENT '定价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '在馆库存',
  `borrowed` INT NOT NULL DEFAULT 0 COMMENT '已借出数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '图书简介',
  `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '封面图地址：站内相对路径或完整URL',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除: 0未删 1已删',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_isbn` (`isbn`),
  KEY `idx_name` (`name`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 借阅记录表（归还只更新状态，不删除记录，作为统计与审计基础）
CREATE TABLE IF NOT EXISTS `borrow_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '借阅人ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `borrow_date` DATETIME NOT NULL COMMENT '借出时间',
  `due_date` DATETIME NOT NULL COMMENT '应还时间（逾期判断依据）',
  `return_date` DATETIME DEFAULT NULL COMMENT '实际归还时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0借出 1已还',
  `renew_count` INT NOT NULL DEFAULT 0 COMMENT '续借次数',
  `overdue` TINYINT NOT NULL DEFAULT 0 COMMENT '逾期标记: 0正常 1逾期未还（定时任务刷新）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`),
  KEY `idx_book` (`book_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- 公告表
CREATE TABLE IF NOT EXISTS `notice` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT COMMENT '公告内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_book` (`user_id`, `book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 操作日志表（AOP 切面写入，供管理员审计查询）
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名快照',
  `module` VARCHAR(32) NOT NULL COMMENT '业务模块：图书/分类/公告/用户/借阅',
  `action` VARCHAR(32) NOT NULL COMMENT '动作：新增/编辑/删除/上下架/借出/归还/续借等',
  `http_method` VARCHAR(8) DEFAULT NULL COMMENT 'HTTP 方法',
  `uri` VARCHAR(200) DEFAULT NULL COMMENT '请求路径',
  `params` TEXT COMMENT '入参快照（JSON，敏感字段已脱敏，超长截断）',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `cost_ms` INT DEFAULT NULL COMMENT '耗时（毫秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user` (`user_id`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
