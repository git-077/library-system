package com.library.module.borrow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.common.constant.RoleConstants;
import com.library.common.exception.BizException;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.book.service.BookService;
import com.library.module.borrow.dto.OverdueCheckVO;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.mapper.BorrowMapper;
import com.library.module.borrow.service.BorrowQueryService;
import com.library.module.borrow.service.BorrowService;
import com.library.module.borrow.service.DueSoonPolicy;
import com.library.module.notice.entity.Notice;
import com.library.module.notice.service.NoticeService;
import com.library.module.notification.NotificationType;
import com.library.module.notification.service.NotificationService;
import com.library.module.reservation.entity.Reservation;
import com.library.module.reservation.service.ReservationService;
import com.library.module.user.entity.User;
import com.library.module.user.service.UserService;
import com.library.common.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 借阅服务实现
 *
 * 关键设计：借书 / 还书均为单事务原子操作。
 * 库存扣减使用条件 UPDATE（WHERE stock > 0），以受影响行数判断成败，
 * 拒绝「先查再改」在并发下的超借问题。
 */
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl extends ServiceImpl<BorrowMapper, BorrowRecord> implements BorrowService {

    /** 站内信正文里的时间格式：精确到分钟，读者不需要秒 */
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    private final BookService bookService;
    private final BookMapper bookMapper;
    private final UserService userService;
    private final NoticeService noticeService;
    private final NotificationService notificationService;
    private final BorrowQueryService borrowQueryService;
    private final ReservationService reservationService;

    @Value("${library.borrow.max-count:5}")
    private int maxCount;

    @Value("${library.borrow.days:30}")
    private int borrowDays;

    @Value("${library.borrow.max-renew:1}")
    private int maxRenew;

    /**
     * 借书事务：校验借阅额度 -> 条件更新库存 -> 写入借阅记录。
     * 任一步失败整体回滚，不会出现「库存已扣但记录缺失」的不一致。
     *
     * <p>预约分支：若这位读者手里有该书的「待取书」预约，说明有一份副本已经被
     * 从 stock 挪进了 held 专门留给他，此时不能走 {@code deductStock}（会把别人能借的副本扣掉），
     * 而是 {@code lendHeld}：把 held 里的那一份转成 borrowed。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void borrowBook(Long userId, Long bookId) {
        // 1. 校验账户状态与借阅额度
        User user = userService.getById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BizException("账户不可用，请联系管理员");
        }
        long activeCount = borrowQueryService.countActive(userId);
        if (activeCount >= maxCount) {
            throw new BizException("已达最大可借数量（" + maxCount + " 本），请先归还部分图书");
        }
        // 逾期未还者禁止再借（逾期按 due_date 实时判断，不依赖定时任务刷的标记）
        if (borrowQueryService.countOverdue(userId) > 0) {
            throw new BizException("存在逾期未还图书，请先归还后再借阅");
        }
        // 2. 有预约就用锁定的副本，没有才扣可借库存（条件更新，受影响行数为 0 即失败）
        Reservation ready = reservationService.findReady(userId, bookId);
        int rows = ready != null ? bookMapper.lendHeld(bookId) : bookMapper.deductStock(bookId);
        if (rows == 0) {
            throw new BizException(ready != null
                    ? "为你保留的副本状态异常，请刷新后重试"
                    : "库存不足或图书已下架，借阅失败");
        }
        // 3. 写入借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowDate(LocalDateTime.now());
        record.setDueDate(LocalDateTime.now().plusDays(borrowDays));
        record.setStatus(0);
        record.setRenewCount(0);
        save(record);
        // 4. 预约转借阅（终态，同时让出 active_flag 唯一约束位）
        if (ready != null) {
            reservationService.fulfill(ready.getId());
        }
    }

    /**
     * 还书事务：更新记录状态 -> 回写库存 -> 有排队则把副本锁定给队首并发到书通知。
     *
     * <p>顺序很重要：副本先回到 stock（可借），再决定是否转给 held（锁定）。
     * 反过来的话，在「回库」和「锁定」之间会有一瞬间别人能抢先借走，
     * 队列里的读者就白等一轮。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBook(Long recordId, LoginUser operator) {
        BorrowRecord record = getById(recordId);
        if (record == null) {
            throw new BizException("借阅记录不存在");
        }
        if (record.getStatus() == 1) {
            throw new BizException("该图书已归还");
        }
        checkOwner(record, operator);
        // 先回写库存（条件更新防重复归还）
        int rows = bookMapper.returnStock(record.getBookId());
        if (rows == 0) {
            throw new BizException("库存回写失败，请重试");
        }
        boolean updated = update(new LambdaUpdateWrapper<BorrowRecord>()
                .eq(BorrowRecord::getId, recordId)
                .eq(BorrowRecord::getStatus, 0)
                .set(BorrowRecord::getStatus, 1)
                .set(BorrowRecord::getReturnDate, LocalDateTime.now()));
        if (!updated) {
            throw new BizException("归还失败，请重试");
        }
        // 队列里有人等这本书：把刚回馆的副本锁定给队首，并给他发到书通知
        reservationService.handOff(record.getBookId());
    }

    /** 续借：未逾期、未超续借次数，应还时间顺延一个借期 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void renewBook(Long recordId, LoginUser operator) {
        BorrowRecord record = getById(recordId);
        if (record == null || record.getStatus() == 1) {
            throw new BizException("借阅记录不存在或已归还");
        }
        checkOwner(record, operator);
        if (record.getRenewCount() != null && record.getRenewCount() >= maxRenew) {
            throw new BizException("续借次数已用完");
        }
        if (record.getDueDate().isBefore(LocalDateTime.now())) {
            throw new BizException("图书已逾期，无法续借");
        }
        update(new LambdaUpdateWrapper<BorrowRecord>()
                .eq(BorrowRecord::getId, recordId)
                .set(BorrowRecord::getDueDate, record.getDueDate().plusDays(borrowDays))
                .set(BorrowRecord::getOverdue, 0)
                .setSql("renew_count = renew_count + 1"));
    }

    /**
     * 逾期扫描：
     * 1) 在借且已过应还时间 → overdue = 1；续借后未到期 → overdue = 0（自愈）；
     * 2) 有逾期则发布当日催还公告，同一天只发一条（幂等）。
     *
     * <p>标记是「派生状态」，扫描前不依赖它做业务判断（借书时仍按 due_date 实时判定），
     * 因此即使定时任务某天没跑，也不会误放过逾期读者。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OverdueCheckVO checkOverdue() {
        LocalDateTime now = LocalDateTime.now();
        int marked = baseMapper.markOverdue(now);
        int cleared = baseMapper.clearOverdue(now);

        List<BorrowRecord> overdueList = lambdaQuery()
                .eq(BorrowRecord::getStatus, 0)
                .lt(BorrowRecord::getDueDate, now)
                .orderByAsc(BorrowRecord::getDueDate)
                .list();
        fillInfo(overdueList);

        OverdueCheckVO vo = new OverdueCheckVO();
        vo.setMarkedCount(marked);
        vo.setClearedCount(cleared);
        vo.setOverdueCount(overdueList.size());

        if (!overdueList.isEmpty()) {
            String title = "逾期催还提醒（" + LocalDate.now() + "）";
            boolean exists = noticeService.count(
                    new LambdaQueryWrapper<Notice>().eq(Notice::getTitle, title)) > 0;
            if (!exists) {
                Notice notice = new Notice();
                notice.setTitle(title);
                notice.setContent(buildOverdueContent(overdueList, now));
                noticeService.save(notice);
                vo.setNoticeCreated(true);
                vo.setNoticeTitle(title);
            }
        }
        return vo;
    }

    /** 催还公告正文：最多列 10 条明细，超出用「等 N 本」收口，避免公告过长 */
    private String buildOverdueContent(List<BorrowRecord> overdueList, LocalDateTime now) {
        StringBuilder sb = new StringBuilder();
        sb.append("截至 ").append(now.toLocalDate())
                .append("，共有 ").append(overdueList.size()).append(" 本图书逾期未还，请尽快归还：\n");
        int limit = Math.min(overdueList.size(), 10);
        for (int i = 0; i < limit; i++) {
            BorrowRecord r = overdueList.get(i);
            long days = ChronoUnit.DAYS.between(r.getDueDate().toLocalDate(), now.toLocalDate());
            sb.append("· ").append(Objects.toString(r.getBookName(), "图书#" + r.getBookId()))
                    .append("（读者 ").append(Objects.toString(r.getUsername(), "-")).append("）")
                    .append("已逾期 ").append(Math.max(days, 1)).append(" 天\n");
        }
        if (overdueList.size() > limit) {
            sb.append("等 ").append(overdueList.size()).append(" 本，详见借阅管理。");
        }
        return sb.toString();
    }

    /**
     * 到期前提醒：给「还有 N 天内到期」的在借记录逐条发站内信。
     *
     * <p>幂等键 = {@code DUE_SOON:借阅ID:应还日期}。为什么把日期也放进 key：
     * 读者续借后应还日期变了，应该再提醒一次；而同一个应还日期绝不能每天提醒一次，
     * 否则一本快到期的书会连发 3 条，直接变成骚扰。
     *
     * <p>不加事务：每条提醒相互独立，一条失败不该把其它人的提醒一起回滚。
     */
    @Override
    public int sendDueSoonReminders(int aheadDays) {
        LocalDateTime now = LocalDateTime.now();
        List<BorrowRecord> dueSoon = lambdaQuery()
                .eq(BorrowRecord::getStatus, 0)
                // 已逾期的走催还公告，不在这里重复提醒
                .ge(BorrowRecord::getDueDate, now)
                .le(BorrowRecord::getDueDate, now.plusDays(aheadDays))
                .orderByAsc(BorrowRecord::getDueDate)
                .list();
        fillInfo(dueSoon);

        int sent = 0;
        for (BorrowRecord r : dueSoon) {
            // 双重保险：SQL 已按窗口过滤，这里再判一次，避免将来改 SQL 时把窗口条件丢掉
            if (!DueSoonPolicy.inWindow(r.getDueDate(), now, aheadDays)) {
                continue;
            }
            String name = Objects.toString(r.getBookName(), "图书#" + r.getBookId());
            long remain = DueSoonPolicy.remainDays(r.getDueDate(), now);
            String title = DueSoonPolicy.title(name, remain);
            String content = "《" + name + "》应于 " + r.getDueDate().format(MINUTE) + " 前归还"
                    + (remain <= 0 ? "（今天最后一天）" : "，还剩 " + remain + " 天")
                    + "。如需继续阅读，可续借 " + maxRenew + " 次（每次 " + borrowDays + " 天）。";
            boolean ok = notificationService.send(r.getUserId(), NotificationType.DUE_SOON,
                    title, content, r.getId(),
                    DueSoonPolicy.dedupeKey(r.getId(), r.getDueDate()));
            if (ok) {
                sent++;
            }
        }
        return sent;
    }

    private void checkOwner(BorrowRecord record, LoginUser operator) {
        boolean isOwner = operator.getUserId().equals(record.getUserId());
        if (!isOwner && !RoleConstants.ADMIN.equalsIgnoreCase(operator.getRole())) {
            throw new BizException(403, "无权操作他人借阅记录");
        }
    }

    @Override
    public IPage<BorrowRecord> pageMy(Long userId, long pageNum, long pageSize, Integer status) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(status != null, BorrowRecord::getStatus, status)
                .orderByDesc(BorrowRecord::getId);
        IPage<BorrowRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillInfo(page.getRecords());
        return page;
    }

    @Override
    public IPage<BorrowRecord> pageAll(long pageNum, long pageSize, Integer status) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .eq(status != null, BorrowRecord::getStatus, status)
                .orderByDesc(BorrowRecord::getId);
        IPage<BorrowRecord> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillInfo(page.getRecords());
        return page;
    }

    @Override
    public List<BorrowRecord> listForExport(Integer status) {
        List<BorrowRecord> records = lambdaQuery()
                .eq(status != null, BorrowRecord::getStatus, status)
                .orderByAsc(BorrowRecord::getId)
                .list();
        fillInfo(records);
        return records;
    }

    /** 批量填充书名与借阅人用户名（避免 N+1 查询） */
    private void fillInfo(List<BorrowRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> bookIds = records.stream().map(BorrowRecord::getBookId).distinct().toList();
        List<Long> userIds = records.stream().map(BorrowRecord::getUserId).distinct().toList();
        Map<Long, String> bookNames = bookService.listByIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Book::getName));
        Map<Long, String> usernames = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        records.forEach(r -> {
            r.setBookName(bookNames.get(r.getBookId()));
            r.setUsername(usernames.get(r.getUserId()));
        });
    }
}
