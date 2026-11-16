-- =============================================
-- 图书管理系统初始化脚本
-- 由 docker-compose 自动挂载到 MySQL 首次启动执行
-- 默认账号由后端 DataInitializer 创建（首次启动时）：
--   admin / admin123  （管理员）
--   reader / 123456   （读者）
--   reader2 / 123456  （读者）
-- =============================================

CREATE DATABASE IF NOT EXISTS library_system_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE library_system_db;

-- 用户表
CREATE TABLE `user` (
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
CREATE TABLE `category` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- 图书表（软删除，stock 在馆库存 + borrowed 已借出 = 总藏书量）
CREATE TABLE `book` (
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
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除: 0未删 1已删',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_isbn` (`isbn`),
  KEY `idx_name` (`name`),
  KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 借阅记录表（归还时只更新状态，不删除记录，作为审计与统计数据基础）
CREATE TABLE `borrow_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '借阅人ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `borrow_date` DATETIME NOT NULL COMMENT '借出时间',
  `due_date` DATETIME NOT NULL COMMENT '应还时间（逾期判断依据）',
  `return_date` DATETIME DEFAULT NULL COMMENT '实际归还时间',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0借出 1已还',
  `renew_count` INT NOT NULL DEFAULT 0 COMMENT '续借次数',
  `overdue` TINYINT NOT NULL DEFAULT 0 COMMENT '逾期标记: 0正常 1逾期未还（每日定时任务刷新）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user` (`user_id`),
  KEY `idx_book` (`book_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- 公告表
CREATE TABLE `notice` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT COMMENT '公告内容',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告表';

-- 收藏表
CREATE TABLE `favorite` (
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
  `params` TEXT COMMENT '入参快照（JSON，密码等敏感字段已脱敏，超长截断）',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `cost_ms` INT DEFAULT NULL COMMENT '耗时（毫秒）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user` (`user_id`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============ 初始数据 ============

INSERT INTO `category` (`name`, `description`) VALUES
('计算机', '编程、算法、数据库、网络等技术类图书'),
('文学', '小说、散文、诗歌等文学作品'),
('历史', '中外历史、人物传记'),
('科技', '科普、自然科学类图书');

INSERT INTO `book` (`isbn`, `name`, `author`, `publisher`, `category_id`, `price`, `stock`, `borrowed`, `status`, `description`) VALUES
('9787111213828', '深入理解Java虚拟机', '周志明', '机械工业出版社', 1, 129.00, 5, 0, 1, 'Java开发者必读经典，全面讲解JVM原理与调优。'),
('9787115546081', 'Vue.js设计与实现', '霍春阳', '人民邮电出版社', 1, 99.80, 3, 0, 1, '剖析Vue 3框架的设计与源码实现。'),
('9787115479013', 'MySQL必知必会', 'Ben Forta', '人民邮电出版社', 1, 49.00, 8, 0, 1, 'MySQL入门与实操指南。'),
('9787020002207', '红楼梦', '曹雪芹', '人民文学出版社', 2, 59.00, 6, 0, 1, '中国古典四大名著之一。'),
('9787532776469', '百年孤独', '加西亚·马尔克斯', '上海译文出版社', 2, 55.00, 4, 0, 1, '魔幻现实主义文学代表作。'),
('9787101003048', '史记', '司马迁', '中华书局', 3, 128.00, 3, 0, 1, '中国历史上第一部纪传体通史。'),
('9787536692930', '时间简史', '史蒂芬·霍金', '湖南科学技术出版社', 4, 45.00, 5, 0, 1, '风靡全球的科普经典。'),
('9787544291170', '人类简史', '尤瓦尔·赫拉利', '中信出版社', 4, 68.00, 4, 0, 1, '从认知革命到人工智能的人类发展史。');

INSERT INTO `notice` (`title`, `content`) VALUES
('欢迎使用图书管理系统', '系统已上线，读者可在线检索图书、发起借阅与归还。每人最多同时借阅 5 本，借期 30 天，可续借 1 次。如有问题请联系管理员。');
