package com.library.module.reservation.service;

import com.library.common.exception.BizException;
import com.library.common.security.LoginUser;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.book.service.BookService;
import com.library.module.borrow.service.BorrowQueryService;
import com.library.module.notification.NotificationType;
import com.library.module.notification.service.NotificationService;
import com.library.module.reservation.constant.ReservationStatus;
import com.library.module.reservation.entity.Reservation;
import com.library.module.reservation.mapper.ReservationMapper;
import com.library.module.reservation.service.impl.ReservationServiceImpl;
import com.library.module.user.service.UserService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约排队的核心不变量（Mockito 单测，不连库）。
 *
 * <p>这里只挑「代码看着没问题、线上却会出事」的三条路径：
 * <ol>
 *   <li>没有可锁定副本时不能让位成功 —— 否则读者收到「到馆」通知却借不到书；</li>
 *   <li>队首刚好被别人取消时要归还刚锁定的副本 —— 否则 {@code book.held} 永久泄漏一份馆藏；</li>
 *   <li>取消「待取书」必须释放副本并顺延 —— 否则队列卡死。</li>
 * </ol>
 */
@DisplayName("预约排队：让位与取消的库存不变量")
class ReservationServiceImplTest {

    private BookService bookService;
    private BookMapper bookMapper;
    private UserService userService;
    private NotificationService notificationService;
    private BorrowQueryService borrowQueryService;
    private ReservationMapper reservationMapper;
    private ReservationServiceImpl service;

    /**
     * LambdaUpdateWrapper 的 {@code set(Entity::getXxx, ...)} 要把方法引用解析成列名，
     * 依赖 MyBatis-Plus 的 TableInfo 缓存。纯单测里没有 MyBatis 上下文，必须先手工初始化，
     * 否则会在构造 wrapper 时抛「can not find lambda cache」。
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Reservation.class);
    }

    @BeforeEach
    void setUp() {
        bookService = mock(BookService.class);
        bookMapper = mock(BookMapper.class);
        userService = mock(UserService.class);
        notificationService = mock(NotificationService.class);
        borrowQueryService = mock(BorrowQueryService.class);
        reservationMapper = mock(ReservationMapper.class);

        service = new ReservationServiceImpl(bookService, bookMapper, userService,
                notificationService, borrowQueryService);
        // ServiceImpl 的 baseMapper 正常情况下由 MyBatis-Plus 注入，单测里手动塞进去
        ReflectionTestUtils.setField(service, "baseMapper", reservationMapper);
        ReflectionTestUtils.setField(service, "holdDays", 7);
        ReflectionTestUtils.setField(service, "maxCount", 3);
    }

    private static Reservation reservation(Long id, Long userId, Long bookId, int status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId(userId);
        r.setBookId(bookId);
        r.setStatus(status);
        r.setActiveFlag(1);
        return r;
    }

    @Test
    @DisplayName("队列为空时让位什么都不做：不锁副本、不发通知")
    void handOffDoesNothingWhenQueueEmpty() {
        when(reservationMapper.selectOne(any())).thenReturn(null);

        service.handOff(3L);

        verify(bookMapper, never()).holdCopy(anyLong());
        verify(bookMapper, never()).releaseHold(anyLong());
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("没有可锁定副本时放弃让位：预约留在排队中，等下一次归还")
    void handOffGivesUpWhenNoCopyToHold() {
        when(reservationMapper.selectOne(any())).thenReturn(reservation(11L, 7L, 3L, ReservationStatus.WAITING));
        // 副本已被并发借走
        when(bookMapper.holdCopy(3L)).thenReturn(0);

        service.handOff(3L);

        // 关键：不能把预约改成「待取书」，否则读者收到通知却借不到
        verify(reservationMapper, never()).update(any(), any());
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("队首刚被取消时把已锁定的副本还回去，避免 held 永久泄漏")
    void handOffReleasesCopyWhenHeadVanished() {
        when(reservationMapper.selectOne(any())).thenReturn(reservation(12L, 7L, 3L, ReservationStatus.WAITING));
        when(bookMapper.holdCopy(3L)).thenReturn(1);
        // 条件更新 (status = WAITING) 抢不到，说明队首刚取消
        when(reservationMapper.update(isNull(), any())).thenReturn(0);

        service.handOff(3L);

        verify(bookMapper).releaseHold(3L);
        verify(notificationService, never()).send(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("正常让位：锁一份副本给队首，发到书通知且幂等键按预约ID生成")
    void handOffLocksCopyAndNotifiesHead() {
        when(reservationMapper.selectOne(any())).thenReturn(reservation(13L, 7L, 3L, ReservationStatus.WAITING));
        when(bookMapper.holdCopy(3L)).thenReturn(1);
        when(reservationMapper.update(isNull(), any())).thenReturn(1);
        Book book = new Book();
        book.setId(3L);
        book.setName("百年孤独");
        when(bookService.getById(3L)).thenReturn(book);

        service.handOff(3L);

        verify(bookMapper, never()).releaseHold(anyLong());
        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dedupe = ArgumentCaptor.forClass(String.class);
        verify(notificationService).send(eq(7L), eq(NotificationType.RESERVATION_READY),
                title.capture(), content.capture(), eq(13L), dedupe.capture());

        assertThat(title.getValue()).contains("百年孤独").contains("预约到书");
        assertThat(content.getValue()).contains("已到馆").contains("保留至");
        // 幂等键不带随机成分：任务重跑不会重复发信
        assertThat(dedupe.getValue()).isEqualTo("RESERVATION_READY:13");
    }

    @Test
    @DisplayName("取消「排队中」只改状态，不碰库存")
    void cancelWaitingDoesNotTouchStock() {
        Reservation r = reservation(21L, 9L, 3L, ReservationStatus.WAITING);
        when(reservationMapper.selectById(21L)).thenReturn(r);
        when(reservationMapper.update(isNull(), any())).thenReturn(1);

        service.cancel(21L, new LoginUser(9L, "reader", "user"));

        verify(bookMapper, never()).releaseHold(anyLong());
        verify(bookMapper, never()).holdCopy(anyLong());
    }

    @Test
    @DisplayName("取消「待取书」释放锁定副本并顺延给下一位")
    void cancelReadyReleasesHold() {
        Reservation r = reservation(22L, 9L, 3L, ReservationStatus.READY);
        when(reservationMapper.selectById(22L)).thenReturn(r);
        when(reservationMapper.update(isNull(), any())).thenReturn(1);
        when(bookMapper.releaseHold(3L)).thenReturn(1);
        // 队列里已无人在等
        when(reservationMapper.selectOne(any())).thenReturn(null);

        service.cancel(22L, new LoginUser(9L, "reader", "user"));

        verify(bookMapper).releaseHold(3L);
        verify(bookMapper, never()).holdCopy(anyLong());
    }

    @Test
    @DisplayName("读者不能取消别人的预约")
    void cancelOthersReservationIsForbidden() {
        Reservation r = reservation(23L, 9L, 3L, ReservationStatus.WAITING);
        when(reservationMapper.selectById(23L)).thenReturn(r);

        assertThatThrownBy(() -> service.cancel(23L, new LoginUser(99L, "other", "user")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("无权");
        verify(reservationMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("预约态回显：没有预约时 myStatus 为 null，按钮走「预约排队」分支")
    void bookStatusWithoutReservation() {
        when(reservationMapper.selectCount(any())).thenReturn(0L);
        when(reservationMapper.selectOne(any())).thenReturn(null);

        var vo = service.bookStatus(9L, 3L);

        assertThat(vo.getMyStatus()).isNull();
        assertThat(vo.getWaitingCount()).isZero();
        assertThat(vo.getHoldDays()).isEqualTo(7);
    }
}
