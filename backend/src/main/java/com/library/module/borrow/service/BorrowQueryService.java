package com.library.module.borrow.service;

/**
 * 借阅数据<strong>只读</strong>查询：给「预约」等其它模块用的窄接口。
 *
 * <p>为什么单独拆一个接口而不是直接依赖 {@link BorrowService}：
 * 还书时要通知预约队列（BorrowService → ReservationService），
 * 而预约又要判断「是否已在借 / 是否有逾期」（ReservationService → BorrowService），
 * 两边直连就成了循环依赖。把只读能力拆出来，
 * 依赖方向变成 ReservationService → BorrowQueryService，BorrowService → ReservationService，
 * 无环，也不需要 {@code @Lazy} 这类补丁。
 *
 * <p>同时它也是「借阅规则口径」的唯一出处：借书时的额度/逾期判断也走这里，
 * 避免预约和借书对「什么是逾期」理解不一致。
 */
public interface BorrowQueryService {

    /** 当前在借数量（status = 0） */
    long countActive(Long userId);

    /** 逾期未还数量（在借且已过应还时间） */
    long countOverdue(Long userId);

    /** 是否正在借这本书（同一本书不能既借又预约） */
    boolean hasActiveBorrow(Long userId, Long bookId);

    /** 是否存在逾期未还（预约准入用，语义比 countOverdue > 0 直白） */
    default boolean hasOverdue(Long userId) {
        return countOverdue(userId) > 0;
    }
}
