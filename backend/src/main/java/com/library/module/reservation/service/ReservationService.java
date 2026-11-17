package com.library.module.reservation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.library.common.security.LoginUser;
import com.library.module.reservation.dto.BookReservationVO;
import com.library.module.reservation.entity.Reservation;

import java.util.List;

/**
 * 预约排队服务。
 *
 * <p>库存口径（改这里之前先读一遍）：
 * <pre>
 *   book.stock    = 任何人都能借的副本
 *   book.held     = 被「待取书」预约锁定的副本（别人借不走）
 *   book.borrowed = 已借出
 *   stock + held + borrowed = 馆藏总量
 * </pre>
 * 「还书 → 有人排队 → 副本不进 stock 而进 held」是本模块与借阅模块之间唯一的耦合点，
 * 由 {@link #handOff(Long)} 承担；借书时消耗 locked 副本由 {@code BookMapper.lendHeld} 承担。
 */
public interface ReservationService extends IService<Reservation> {

    /** 预约（仅当无可借库存时允许），返回预约 ID */
    Long reserve(Long userId, Long bookId);

    /** 取消预约：本人或管理员；若已锁定副本，会释放并顺延给下一位 */
    void cancel(Long reservationId, LoginUser operator);

    /** 我的预约列表（含排队位置） */
    List<Reservation> myList(Long userId);

    /** 管理员分页：可按状态与书名筛选 */
    IPage<Reservation> pageAdmin(long pageNum, long pageSize, Integer status, String keyword);

    /** 图书详情页的预约态（队列长度 + 我的状态） */
    BookReservationVO bookStatus(Long userId, Long bookId);

    /* -------- 以下由借阅模块调用，不要在前端接口里直接暴露 -------- */

    /** 查我这本书的「待取书」预约（未过保留期），借书时用 */
    Reservation findReady(Long userId, Long bookId);

    /** 预约转借阅：状态置「已借出」，同时释放 active_flag */
    void fulfill(Long reservationId);

    /** 副本回馆后让位给队首：锁定一份副本 + 发到书通知，队列为空则不做事 */
    void handOff(Long bookId);

    /** 清扫过期「待取书」：释放锁定副本并顺延，返回处理条数 */
    int sweepExpired();
}
