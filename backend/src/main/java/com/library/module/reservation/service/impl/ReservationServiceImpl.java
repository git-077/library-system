package com.library.module.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.common.constant.RoleConstants;
import com.library.common.exception.BizException;
import com.library.common.security.LoginUser;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.book.service.BookService;
import com.library.module.borrow.service.BorrowQueryService;
import com.library.module.notification.NotificationType;
import com.library.module.notification.service.NotificationService;
import com.library.module.reservation.constant.ReservationStatus;
import com.library.module.reservation.dto.BookReservationVO;
import com.library.module.reservation.entity.Reservation;
import com.library.module.reservation.mapper.ReservationMapper;
import com.library.module.reservation.service.ReservationService;
import com.library.module.user.entity.User;
import com.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 预约排队实现。
 *
 * <p>三个关键不变量（并发下也必须成立）：
 * <ol>
 *   <li>进行中的预约每人每书最多一条 —— 由唯一索引 {@code uk_active} 兜底，不靠「先查后插」；</li>
 *   <li>{@code book.held} 永远等于「未过期的待取书预约数」—— 锁定和释放都用带条件的 UPDATE，
 *       受影响行数为 0 就说明状态已被别人改动，立即放弃而不是硬改；</li>
 *   <li>队列判定只看状态 + 自增 ID，不看时间戳 —— 同一秒多人预约时时间戳分不出先后。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation>
        implements ReservationService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("M月d日");

    private final BookService bookService;
    private final BookMapper bookMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final BorrowQueryService borrowQueryService;

    /** 到馆后保留天数 */
    @Value("${library.reservation.hold-days:7}")
    private int holdDays;

    /** 同时进行中的预约上限（防止一个人把整排热门书都占住） */
    @Value("${library.reservation.max-count:3}")
    private int maxCount;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reserve(Long userId, Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null || book.getStatus() == null || book.getStatus() == 0) {
            throw new BizException("图书不存在或已下架");
        }
        if (book.getStock() != null && book.getStock() > 0) {
            // 有可借副本时预约没有意义，还会把队列搅乱
            throw new BizException("当前有可借库存，无需预约，可直接借阅");
        }
        if (borrowQueryService.hasOverdue(userId)) {
            throw new BizException("存在逾期未还图书，请先归还后再预约");
        }
        if (borrowQueryService.hasActiveBorrow(userId, bookId)) {
            throw new BizException("你正在借阅这本书，无需预约");
        }
        long active = lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .in(Reservation::getStatus, ReservationStatus.WAITING, ReservationStatus.READY)
                .count();
        if (active >= maxCount) {
            throw new BizException("最多同时保留 " + maxCount + " 个预约，请先取消不想要的");
        }

        Reservation r = new Reservation();
        r.setUserId(userId);
        r.setBookId(bookId);
        r.setStatus(ReservationStatus.WAITING);
        r.setActiveFlag(1);
        r.setCreateTime(LocalDateTime.now());
        try {
            save(r);
        } catch (DuplicateKeyException e) {
            // 唯一索引 (user_id, book_id, active_flag)：连点两次或并发提交时命中这里
            throw new BizException("你已预约过这本书，请勿重复提交");
        }
        return r.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long reservationId, LoginUser operator) {
        Reservation r = getById(reservationId);
        if (r == null) {
            throw new BizException("预约记录不存在");
        }
        if (!ReservationStatus.isActive(r.getStatus())) {
            throw new BizException("该预约已结束，无需取消");
        }
        boolean isOwner = operator != null && Objects.equals(operator.getUserId(), r.getUserId());
        if (!isOwner && (operator == null || !RoleConstants.ADMIN.equalsIgnoreCase(operator.getRole()))) {
            throw new BizException(403, "无权取消他人的预约");
        }

        boolean ok = update(new LambdaUpdateWrapper<Reservation>()
                .eq(Reservation::getId, reservationId)
                .in(Reservation::getStatus, ReservationStatus.WAITING, ReservationStatus.READY)
                .set(Reservation::getStatus, ReservationStatus.CANCELLED)
                .set(Reservation::getActiveFlag, null)
                .set(Reservation::getFinishTime, LocalDateTime.now()));
        if (!ok) {
            throw new BizException("预约状态已变化，请刷新后重试");
        }

        // 只有「待取书」才占着副本，取消时必须把副本还给下一位（或还回库存）
        if (r.getStatus() == ReservationStatus.READY) {
            if (bookMapper.releaseHold(r.getBookId()) == 0) {
                log.warn("释放锁定副本失败，book.held 可能已为 0：bookId={} reservationId={}",
                        r.getBookId(), reservationId);
            }
            handOff(r.getBookId());
        }
    }

    @Override
    public List<Reservation> myList(Long userId) {
        List<Reservation> list = lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getId)
                .list();
        fillInfo(list, false);
        return list;
    }

    @Override
    public IPage<Reservation> pageAdmin(long pageNum, long pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(status != null, Reservation::getStatus, status)
                .orderByDesc(Reservation::getId);
        if (StringUtils.hasText(keyword)) {
            // 先按书名把 book_id 捞出来，再进主查询：比 join 更好维护，也复用图书检索的模糊规则
            List<Long> bookIds = bookService.lambdaQuery()
                    .like(Book::getName, keyword.trim())
                    .list()
                    .stream().map(Book::getId).toList();
            if (bookIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
            wrapper.in(Reservation::getBookId, bookIds);
        }
        IPage<Reservation> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillInfo(page.getRecords(), true);
        return page;
    }

    @Override
    public BookReservationVO bookStatus(Long userId, Long bookId) {
        BookReservationVO vo = new BookReservationVO();
        vo.setHoldDays(holdDays);
        vo.setWaitingCount(lambdaQuery()
                .eq(Reservation::getBookId, bookId)
                .eq(Reservation::getStatus, ReservationStatus.WAITING)
                .count());
        vo.setReadyCount(lambdaQuery()
                .eq(Reservation::getBookId, bookId)
                .eq(Reservation::getStatus, ReservationStatus.READY)
                .count());
        Reservation mine = lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getBookId, bookId)
                .in(Reservation::getStatus, ReservationStatus.WAITING, ReservationStatus.READY)
                .last("LIMIT 1")
                .one();
        if (mine != null) {
            vo.setMyStatus(mine.getStatus());
            vo.setMyReservationId(mine.getId());
            if (mine.getStatus() == ReservationStatus.WAITING) {
                vo.setMyPosition(baseMapper.countAhead(bookId, mine.getId()) + 1);
            } else {
                vo.setMyExpireTime(mine.getExpireTime());
            }
        }
        return vo;
    }

    @Override
    public Reservation findReady(Long userId, Long bookId) {
        return lambdaQuery()
                .eq(Reservation::getUserId, userId)
                .eq(Reservation::getBookId, bookId)
                .eq(Reservation::getStatus, ReservationStatus.READY)
                // 过期但还没被清扫的，不能拿来借：否则会绕过「保留 7 天」的规则
                .gt(Reservation::getExpireTime, LocalDateTime.now())
                .orderByAsc(Reservation::getId)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public void fulfill(Long reservationId) {
        update(new LambdaUpdateWrapper<Reservation>()
                .eq(Reservation::getId, reservationId)
                .eq(Reservation::getStatus, ReservationStatus.READY)
                .set(Reservation::getStatus, ReservationStatus.FULFILLED)
                .set(Reservation::getActiveFlag, null)
                .set(Reservation::getFinishTime, LocalDateTime.now()));
    }

    /**
     * 让位：把一份回馆的副本锁定给队首，并发到书通知。
     *
     * <p>执行顺序不能反：先锁副本（stock → held），再改预约状态。
     * 反过来会出现「预约已变成待取书、但 held 没加上」的窗口期，
     * 此时别人能把这个副本借走，预约者拿到通知却借不到。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handOff(Long bookId) {
        Reservation next = lambdaQuery()
                .eq(Reservation::getBookId, bookId)
                .eq(Reservation::getStatus, ReservationStatus.WAITING)
                .orderByAsc(Reservation::getId)
                .last("LIMIT 1")
                .one();
        if (next == null) {
            return;
        }
        if (bookMapper.holdCopy(bookId) == 0) {
            // 副本已被别人借走（并发归还 + 借出），本次放弃，下次归还再让位
            log.debug("无可锁定副本，跳过让位：bookId={}", bookId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expire = now.plusDays(holdDays);
        boolean ok = update(new LambdaUpdateWrapper<Reservation>()
                .eq(Reservation::getId, next.getId())
                .eq(Reservation::getStatus, ReservationStatus.WAITING)
                .set(Reservation::getStatus, ReservationStatus.READY)
                .set(Reservation::getReadyTime, now)
                .set(Reservation::getExpireTime, expire));
        if (!ok) {
            // 队首刚被别人取消，把刚锁定的副本还回去，避免 held 泄漏
            bookMapper.releaseHold(bookId);
            return;
        }

        Book book = bookService.getById(bookId);
        String name = book == null ? "图书#" + bookId : book.getName();
        notificationService.send(next.getUserId(), NotificationType.RESERVATION_READY,
                "预约到书：《" + name + "》",
                "你预约的《" + name + "》已到馆，并为你保留至 " + expire.format(DAY)
                        + "。请在此之前到馆借阅，逾期将自动顺延给下一位。",
                next.getId(), "RESERVATION_READY:" + next.getId());
    }

    /**
     * 清扫过期预约：保留期到了还没来借，状态置「已过期」，副本让给下一位。
     *
     * <p>每行都先做条件更新（{@code status = READY} 才改），抢到才释放副本，
     * 因此两台机器同时扫也不会把一份副本释放两次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int sweepExpired() {
        List<Reservation> expired = lambdaQuery()
                .eq(Reservation::getStatus, ReservationStatus.READY)
                .lt(Reservation::getExpireTime, LocalDateTime.now())
                .orderByAsc(Reservation::getId)
                .last("LIMIT 200")
                .list();
        int handled = 0;
        for (Reservation r : expired) {
            boolean ok = update(new LambdaUpdateWrapper<Reservation>()
                    .eq(Reservation::getId, r.getId())
                    .eq(Reservation::getStatus, ReservationStatus.READY)
                    .set(Reservation::getStatus, ReservationStatus.EXPIRED)
                    .set(Reservation::getActiveFlag, null)
                    .set(Reservation::getFinishTime, LocalDateTime.now()));
            if (!ok) {
                continue;
            }
            if (bookMapper.releaseHold(r.getBookId()) == 0) {
                log.warn("过期清扫释放副本失败：bookId={} reservationId={}", r.getBookId(), r.getId());
            }
            handOff(r.getBookId());

            Book book = bookService.getById(r.getBookId());
            String name = book == null ? "图书#" + r.getBookId() : book.getName();
            notificationService.send(r.getUserId(), NotificationType.RESERVATION_EXPIRED,
                    "预约已过期：《" + name + "》",
                    "《" + name + "》的取书保留期已过，预约已自动取消。如仍需要，可重新预约排队。",
                    r.getId(), "RESERVATION_EXPIRED:" + r.getId());
            handled++;
        }
        return handled;
    }

    /** 批量补书名/作者/封面（管理端再补用户名），并算出排队位置 */
    private void fillInfo(List<Reservation> list, boolean withUsername) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, Book> books = bookService.listByIds(
                        list.stream().map(Reservation::getBookId).distinct().toList())
                .stream().collect(Collectors.toMap(Book::getId, b -> b));
        Map<Long, String> usernames = withUsername
                ? userService.listByIds(list.stream().map(Reservation::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getUsername))
                : Map.of();

        for (Reservation r : list) {
            Book b = books.get(r.getBookId());
            if (b != null) {
                r.setBookName(b.getName());
                r.setBookAuthor(b.getAuthor());
                r.setCoverUrl(b.getCoverUrl());
            }
            if (withUsername) {
                r.setUsername(usernames.get(r.getUserId()));
            }
            if (r.getStatus() != null && r.getStatus() == ReservationStatus.WAITING) {
                r.setPosition(baseMapper.countAhead(r.getBookId(), r.getId()) + 1);
            }
        }
    }
}
