-- =============================================
-- 演示数据（可重复执行，每次都会把这批演示图书重建一遍）
--
-- 用途：给「数据统计 / 馆藏检索 / 图书管理 / 分类管理 / 借阅管理 / 用户管理」
--       六个页面灌入足够的样例数据，方便演示与截图。
--
-- 执行：
--   mysql --default-character-set=utf8mb4 -uroot -p123456 -D library_system_db < sql/seed-demo-data.sql
--
-- 设计要点：
--   1. 分类 id 用「按名称查」的变量取，绝不硬编码 —— 自增 id 会因增删而跳号；
--   2. 每次都按 ISBN 先删后建，脚本跑多少遍结果都一样，不会堆重复数据；
--   3. book.stock（在馆）+ book.borrowed（已借出）与 borrow_record 的在借条数严格一致；
--   4. 新用户密码统一 123456（复用 reader 的 BCrypt 哈希，明文相同即可复用）。
-- =============================================

USE library_system_db;
SET NAMES utf8mb4;

-- ---------- 1. 分类（补 6 个，共 10 个）----------
INSERT IGNORE INTO `category` (`name`, `description`) VALUES
('艺术',     '美术、音乐、设计、摄影'),
('哲学',     '哲学思想、伦理学、逻辑学'),
('经济管理', '经济学、管理学、金融投资'),
('外语',     '英语、日语等语言学习与工具书'),
('教育',     '教育理论、教学方法、家庭教育'),
('医学',     '基础医学、临床、健康科普');

-- ---------- 2. 清理本脚本此前生成的数据（按 ISBN 定位，不依赖自增 id）----------
DROP TEMPORARY TABLE IF EXISTS `demo_isbn`;
-- 显式指定与 book 表一致的排序规则（MySQL 8 默认 utf8mb4_0900_ai_ci），
-- 否则与库级默认 general_ci 混用做 JOIN 会报 Illegal mix of collations
CREATE TEMPORARY TABLE `demo_isbn` (
  `isbn` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci PRIMARY KEY
);
INSERT INTO `demo_isbn` (`isbn`) VALUES
('9787115216878'), ('9787111407010'), ('9787111599715'), ('9787115500865'),
('9787506365437'), ('9787530216781'), ('9787020024759'), ('9787532759295'),
('9787101052039'), ('9787532739235'), ('9787510813085'),
('9787550217287'), ('9787508693243'), ('9787532776711'),
('9787549404988'), ('9787500681620'),
('9787506394680'), ('9787511707321'),
('9787301298980'), ('9787100109796'),
('9787560028903'), ('9787100043563'), ('9787506040853'), ('9787100040944'), ('9787559631925'),
-- 第二批追加的 20 本
('9787111544937'), ('9787115428028'), ('9787111641247'), ('9787115351531'),
('9787020104344'), ('9787530215181'), ('9787532790243'),
('9787213048258'), ('9787301107206'),
('9787100073232'), ('9787115373557'),
('9787108035324'), ('9787115379108'),
('9787301213792'), ('9787100074321'),
('9787508662233'), ('9787508649715'),
('9787560028910'), ('9787550268513'), ('9787508643324');

DELETE f FROM `favorite` f
  JOIN `book` b ON b.id = f.book_id
  JOIN `demo_isbn` d ON d.isbn = b.isbn;
DELETE r FROM `borrow_record` r
  JOIN `book` b ON b.id = r.book_id
  JOIN `demo_isbn` d ON d.isbn = b.isbn;
DELETE b FROM `book` b
  JOIN `demo_isbn` d ON d.isbn = b.isbn;

-- 顺带清掉早期接口自检留下的测试书（不在上面 ISBN 列表里，单独处理）
DELETE FROM `book` WHERE `name` IN ('测试书', '包结构回归测试书');
DELETE r FROM `borrow_record` r
  LEFT JOIN `book` b ON b.id = r.book_id
  WHERE b.id IS NULL;

-- 收藏表没有 book 外键，历史记录可能指向已删除的图书，
-- 清空参与演示的账号的收藏（第 7 步统一重建），并删除孤儿数据
DELETE f FROM `favorite` f
  JOIN `user` u ON u.id = f.user_id
  WHERE u.username IN ('reader','reader3','reader4','reader5','reader6','reader7','reader8','reader9','reader10');
DELETE f FROM `favorite` f
  LEFT JOIN `book` b ON b.id = f.book_id
  WHERE b.id IS NULL;

-- ---------- 3. 分类 id 变量（按名称取，避免自增跳号导致错位）----------
SET @c_cs := (SELECT id FROM `category` WHERE name = '计算机');
SET @c_wx := (SELECT id FROM `category` WHERE name = '文学');
SET @c_ls := (SELECT id FROM `category` WHERE name = '历史');
SET @c_kj := (SELECT id FROM `category` WHERE name = '科技');
SET @c_ys := (SELECT id FROM `category` WHERE name = '艺术');
SET @c_zx := (SELECT id FROM `category` WHERE name = '哲学');
SET @c_jj := (SELECT id FROM `category` WHERE name = '经济管理');
SET @c_wy := (SELECT id FROM `category` WHERE name = '外语');
SET @c_jy := (SELECT id FROM `category` WHERE name = '教育');
SET @c_yx := (SELECT id FROM `category` WHERE name = '医学');

-- ---------- 4. 图书（40 本，覆盖全部分类）----------
-- stock 写「可借数量」，borrowed 写「当前在借数量」，与第 6 步的借阅记录一一对应
INSERT INTO `book`
(`isbn`, `name`, `author`, `publisher`, `category_id`, `price`, `stock`, `borrowed`, `status`, `description`) VALUES
-- 计算机
('9787115216878', '代码整洁之道', 'Robert C. Martin', '人民邮电出版社', @c_cs, 79.00, 3, 1, 1, '如何写出可读、可维护的代码，Clean Code 中文版。'),
('9787111407010', '算法导论', 'Thomas H. Cormen', '机械工业出版社', @c_cs, 128.00, 2, 1, 1, '算法领域的权威教材，覆盖排序、图论、动态规划等。'),
('9787111599715', '计算机网络：自顶向下方法', 'James F. Kurose', '机械工业出版社', @c_cs, 89.00, 5, 0, 1, '从应用层出发讲解网络原理，配套实验丰富。'),
('9787115500865', '重构：改善既有代码的设计', 'Martin Fowler', '人民邮电出版社', @c_cs, 118.00, 3, 0, 1, '系统讲解重构手法与代码坏味道识别。'),
-- 文学
('9787506365437', '活着', '余华', '作家出版社', @c_wx, 28.00, 5, 1, 1, '讲述一个人在时代变迁中的生存与坚韧。'),
('9787530216781', '平凡的世界', '路遥', '北京十月文艺出版社', @c_wx, 108.00, 4, 0, 1, '茅盾文学奖作品，中国当代现实主义长篇代表作。'),
('9787020024759', '围城', '钱锺书', '人民文学出版社', @c_wx, 39.00, 5, 0, 1, '以幽默笔法写尽知识分子的婚姻与人生困境。'),
('9787532759295', '月亮与六便士', '毛姆', '上海译文出版社', @c_wx, 39.00, 4, 0, 1, '关于理想与现实抉择的经典小说。'),
-- 历史
('9787101052039', '万历十五年', '黄仁宇', '中华书局', @c_ls, 26.00, 3, 1, 1, '以大历史观剖析明代政治与社会结构。'),
('9787532739235', '枪炮、病菌与钢铁', '贾雷德·戴蒙德', '上海译文出版社', @c_ls, 68.00, 3, 0, 1, '从地理与生态解释人类文明发展的差异。'),
('9787510813085', '中国历代政治得失', '钱穆', '九州出版社', @c_ls, 32.00, 3, 0, 1, '汉唐宋明清五代政治制度的比较与评析。'),
-- 科技
('9787550217287', '上帝掷骰子吗', '曹天元', '北京联合出版公司', @c_kj, 45.00, 4, 0, 1, '量子物理史话，把科学史写得像小说。'),
('9787508693243', '自私的基因', '理查德·道金斯', '中信出版社', @c_kj, 69.00, 3, 0, 1, '从基因视角重新理解进化与利他行为。'),
-- 这本故意下架，用来演示「图书管理 → 上下架」与「门户只统计上架图书」的口径差异
('9787532776711', '寂静的春天', '蕾切尔·卡逊', '上海译文出版社', @c_kj, 42.00, 3, 0, 0, '环境保护运动的开山之作（演示用：已下架）。'),
-- 艺术
('9787549404988', '艺术的故事', '贡布里希', '广西美术出版社', @c_ys, 280.00, 1, 1, 1, '西方艺术史入门经典，图文并茂。'),
('9787500681620', '西方美术史话', '迟轲', '中国青年出版社', @c_ys, 45.00, 3, 0, 1, '通俗易懂的西方美术发展脉络。'),
-- 哲学
('9787506394680', '苏菲的世界', '乔斯坦·贾德', '作家出版社', @c_zx, 45.00, 3, 1, 1, '以小说形式串起西方哲学史。'),
('9787511707321', '沉思录', '马可·奥勒留', '中央编译出版社', @c_zx, 36.00, 3, 0, 1, '罗马皇帝写给自己的哲学札记。'),
-- 经济管理
('9787301298980', '经济学原理', '曼昆', '北京大学出版社', @c_jj, 128.00, 3, 1, 1, '经济学入门教材，十大原理贯穿全书。'),
('9787100109796', '国富论', '亚当·斯密', '商务印书馆', @c_jj, 68.00, 3, 0, 1, '现代经济学的奠基之作。'),
-- 外语
('9787560028903', '新概念英语（第一册）', 'L.G. Alexander', '外语教学与研究出版社', @c_wy, 45.90, 6, 0, 1, '英语入门经典教材，配套练习与音频。'),
('9787100043563', '牛津高阶英汉双解词典', '霍恩比', '商务印书馆', @c_wy, 169.00, 2, 0, 1, '英语学习者常用工具书。'),
-- 教育
('9787506040853', '给教师的建议', '苏霍姆林斯基', '教育科学出版社', @c_jy, 39.00, 3, 0, 1, '教育经典，面向一线教师的教学建议。'),
('9787100040944', '如何阅读一本书', '莫提默·艾德勒', '商务印书馆', @c_jy, 56.00, 4, 0, 1, '系统讲解阅读的层次与方法。'),
-- 医学
('9787559631925', '人体简史', '比尔·布莱森', '北京联合出版公司', @c_yx, 88.00, 3, 0, 1, '用通俗笔法讲述人体结构与演化。'),
-- ===== 第二批追加 20 本（各分类均匀补充，全部上架、无在借，stock 有高有低）=====
-- 计算机
('9787111544937', '深入理解计算机系统（原书第3版）', 'Randal E. Bryant', '机械工业出版社', @c_cs, 139.00, 6, 0, 1, '从程序员视角讲解计算机系统底层原理，CSAPP 经典教材。'),
('9787115428028', 'Python编程：从入门到实践（第3版）', 'Eric Matthes', '人民邮电出版社', @c_cs, 109.80, 8, 0, 1, '以项目驱动的方式学习 Python，适合零基础入门。'),
('9787111641247', '设计模式：可复用面向对象软件的基础', 'Erich Gamma', '机械工业出版社', @c_cs, 89.00, 4, 0, 1, 'GoF 二十三种设计模式的奠基之作。'),
('9787115351531', '图解HTTP', '上野宣', '人民邮电出版社', @c_cs, 49.00, 5, 0, 1, '用大量插图讲解 HTTP 协议与 Web 通信机制。'),
-- 文学
('9787020104344', '呐喊', '鲁迅', '人民文学出版社', @c_wx, 26.00, 6, 0, 1, '中国现代白话小说的开山之作，收录《狂人日记》等十四篇。'),
('9787530215181', '边城', '沈从文', '北京十月文艺出版社', @c_wx, 32.00, 5, 0, 1, '以湘西风土为背景的抒情小说代表作。'),
('9787532790243', '挪威的森林', '村上春树', '上海译文出版社', @c_wx, 55.00, 4, 0, 1, '关于青春、孤独与失去的长篇代表作。'),
-- 历史
('9787213048258', '明朝那些事儿（第一部）', '当年明月', '浙江人民出版社', @c_ls, 39.80, 7, 0, 1, '以通俗笔法讲述明代三百年历史的畅销读物。'),
('9787301107206', '全球通史：从史前史到21世纪', '斯塔夫里阿诺斯', '北京大学出版社', @c_ls, 128.00, 3, 0, 1, '以全球视野梳理人类文明演进过程的通史教材。'),
-- 科技
('9787100073232', '物种起源', '查尔斯·达尔文', '商务印书馆', @c_kj, 58.00, 4, 0, 1, '进化论奠基之作，现代生物学思想的源头。'),
('9787115373557', '数学之美（第二版）', '吴军', '人民邮电出版社', @c_kj, 69.00, 5, 0, 1, '用通俗语言讲清搜索、翻译背后的数学原理。'),
-- 艺术
('9787108035324', '世界美术名作二十讲', '傅雷', '生活·读书·新知三联书店', @c_ys, 68.00, 3, 0, 1, '傅雷讲解西方美术名作的经典讲稿。'),
('9787115379108', '摄影的艺术', 'Bruce Barnbaum', '人民邮电出版社', @c_ys, 128.00, 2, 0, 1, '从构图、光线到暗房与数字后期的系统摄影教程。'),
-- 哲学
('9787301213792', '中国哲学简史', '冯友兰', '北京大学出版社', @c_zx, 45.00, 4, 0, 1, '面向普通读者的中国哲学发展脉络梳理。'),
('9787100074321', '理想国', '柏拉图', '商务印书馆', @c_zx, 48.00, 3, 0, 1, '西方政治哲学开篇之作，以对话体讨论正义与城邦。'),
-- 经济管理
('9787508662233', '穷查理宝典', '彼得·考夫曼', '中信出版社', @c_jj, 88.00, 4, 0, 1, '查理·芒格的演讲与思维模型合集。'),
('9787508649715', '从0到1：开启商业与未来的秘密', '彼得·蒂尔', '中信出版社', @c_jj, 45.00, 5, 0, 1, '关于创业与创新的思考，强调创造而非复制。'),
-- 外语
('9787560028910', '新概念英语（第二册）', 'L.G. Alexander', '外语教学与研究出版社', @c_wy, 49.90, 6, 0, 1, '英语语法与句型训练经典教材，承接第一册。'),
-- 教育
('9787550268513', '正面管教', '简·尼尔森', '北京联合出版公司', @c_jy, 42.00, 4, 0, 1, '不惩罚也不娇纵的养育方法，家庭教育常用读物。'),
-- 医学
('9787508643324', '癌症传：众病之王', '悉达多·穆克吉', '中信出版社', @c_yx, 98.00, 3, 0, 1, '普利策奖作品，讲述人类与癌症抗争的千年史。');

-- ---------- 5. 用户（补 8 个读者，密码均为 123456）----------
INSERT IGNORE INTO `user` (`username`, `password`, `real_name`, `role`, `status`, `deleted`) VALUES
('reader3',  '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '陈晨',   'user', 1, 0),
('reader4',  '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '李思远', 'user', 1, 0),
('reader5',  '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '王雨桐', 'user', 1, 0),
('reader6',  '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '赵子墨', 'user', 1, 0),
('reader7',  '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '孙嘉禾', 'user', 1, 0),
('reader8',  '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '周晓岚', 'user', 1, 0),
('reader9',  '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '吴亦宁', 'user', 0, 0),
('reader10', '$2a$10$xIeW4yPuM0gYLLeZvZdO1.uBn4MmUCNsj9Vjm4XwgyO1DmZ8VZHce', '郑清和', 'user', 1, 0);

-- ---------- 6. 借阅记录（7 条在借含 1 条逾期 + 7 条已还）----------
-- 6.1 在借（未到期）：6 条
INSERT INTO `borrow_record` (`user_id`, `book_id`, `borrow_date`, `due_date`, `return_date`, `status`, `renew_count`)
SELECT u.id, b.id, NOW() - INTERVAL 5 DAY,  NOW() + INTERVAL 25 DAY, NULL, 0, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787115216878' WHERE u.username = 'reader3'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 12 DAY, NOW() + INTERVAL 18 DAY, NULL, 0, 1
FROM `user` u JOIN `book` b ON b.isbn = '9787506365437' WHERE u.username = 'reader4'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 20 DAY, NOW() + INTERVAL 10 DAY, NULL, 0, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787101052039' WHERE u.username = 'reader5'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 3 DAY,  NOW() + INTERVAL 27 DAY, NULL, 0, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787549404988' WHERE u.username = 'reader6'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 8 DAY,  NOW() + INTERVAL 22 DAY, NULL, 0, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787506394680' WHERE u.username = 'reader7'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 1 DAY,  NOW() + INTERVAL 29 DAY, NULL, 0, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787301298980' WHERE u.username = 'reader8';

-- 6.2 逾期未还：1 条（借出 45 天，应还日已过 15 天），统计页「逾期」指标 +1
INSERT INTO `borrow_record` (`user_id`, `book_id`, `borrow_date`, `due_date`, `return_date`, `status`, `renew_count`)
SELECT u.id, b.id, NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 15 DAY, NULL, 0, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787111407010' WHERE u.username = 'reader3';

-- 6.3 已归还：7 条（有归还时间，不计入在借，只作历史与审计数据）
INSERT INTO `borrow_record` (`user_id`, `book_id`, `borrow_date`, `due_date`, `return_date`, `status`, `renew_count`)
SELECT u.id, b.id, NOW() - INTERVAL 60 DAY,  NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 38 DAY, 1, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787530216781' WHERE u.username = 'reader3'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 75 DAY,  NOW() - INTERVAL 45 DAY, NOW() - INTERVAL 50 DAY, 1, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787020024759' WHERE u.username = 'reader4'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 52 DAY,  NOW() - INTERVAL 22 DAY, NOW() - INTERVAL 25 DAY, 1, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787532739235' WHERE u.username = 'reader5'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 40 DAY,  NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 12 DAY, 1, 1
FROM `user` u JOIN `book` b ON b.isbn = '9787550217287' WHERE u.username = 'reader6'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 95 DAY,  NOW() - INTERVAL 65 DAY, NOW() - INTERVAL 70 DAY, 1, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787508693243' WHERE u.username = 'reader7'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 30 DAY,  NOW(),                       NOW() - INTERVAL 2 DAY,  1, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787511707321' WHERE u.username = 'reader8'
UNION ALL
SELECT u.id, b.id, NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 90 DAY, NOW() - INTERVAL 88 DAY, 1, 0
FROM `user` u JOIN `book` b ON b.isbn = '9787100109796' WHERE u.username = 'reader9';

-- ---------- 7. 收藏（个人中心「我的收藏」）----------
INSERT INTO `favorite` (`user_id`, `book_id`)
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787115216878' WHERE u.username IN ('reader3','reader')
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787111213828' WHERE u.username = 'reader'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787111407010' WHERE u.username = 'reader3'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787506365437' WHERE u.username = 'reader4'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787020002207' WHERE u.username = 'reader4'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787101052039' WHERE u.username = 'reader5'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787549404988' WHERE u.username = 'reader6'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787506394680' WHERE u.username = 'reader7'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787301298980' WHERE u.username = 'reader8'
UNION ALL
SELECT u.id, b.id FROM `user` u JOIN `book` b ON b.isbn = '9787544291170' WHERE u.username = 'reader8';

-- ---------- 8. 一致性自检（三个数字必须都是 0）----------
SELECT
  (SELECT COUNT(*) FROM `book` b
    WHERE b.deleted = 0
      AND b.borrowed <> (SELECT COUNT(*) FROM `borrow_record` r
                          WHERE r.book_id = b.id AND r.status = 0))  AS 库存与在借不一致的图书数,
  (SELECT COUNT(*) FROM `borrow_record` r
     LEFT JOIN `user` u ON u.id = r.user_id
     LEFT JOIN `book` b ON b.id = r.book_id
    WHERE u.id IS NULL OR b.id IS NULL)                              AS 孤儿借阅记录数,
  (SELECT COUNT(*) FROM `book` WHERE stock < 0 OR borrowed < 0)      AS 负库存图书数,
  (SELECT COUNT(*) FROM `book` b
     LEFT JOIN `category` c ON c.id = b.category_id
    WHERE b.deleted = 0 AND c.id IS NULL)                            AS 分类丢失的图书数;

SELECT
  (SELECT COUNT(*) FROM `user` WHERE deleted = 0)                AS 用户数,
  (SELECT COUNT(*) FROM `category`)                              AS 分类数,
  (SELECT COUNT(*) FROM `book` WHERE deleted = 0)                AS 图书数,
  (SELECT COUNT(*) FROM `book` WHERE status = 1 AND deleted = 0) AS 上架图书数,
  (SELECT COUNT(*) FROM `borrow_record` WHERE status = 0)        AS 在借记录数,
  (SELECT COUNT(*) FROM `borrow_record` WHERE status = 0 AND due_date < NOW()) AS 逾期数,
  (SELECT COUNT(*) FROM `favorite`)                              AS 收藏数;
