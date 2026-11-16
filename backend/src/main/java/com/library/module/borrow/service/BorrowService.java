package com.library.module.borrow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.borrow.dto.OverdueCheckVO;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.common.security.LoginUser;

import java.util.List;

public interface BorrowService extends IService<BorrowRecord> {

    /** 借书（单事务：额度校验 -> 条件扣库存 -> 写记录） */
    void borrowBook(Long userId, Long bookId);

    /** 还书（单事务：更新记录 -> 回写库存），本人或管理员可操作 */
    void returnBook(Long recordId, LoginUser operator);

    /** 续借（未逾期且未超次数），本人或管理员可操作 */
    void renewBook(Long recordId, LoginUser operator);

    /** 我的借阅记录（分页） */
    IPage<BorrowRecord> pageMy(Long userId, long pageNum, long pageSize, Integer status);

    /** 全部借阅记录（管理员，分页） */
    IPage<BorrowRecord> pageAll(long pageNum, long pageSize, Integer status);

    /**
     * 逾期扫描：刷新借阅记录的 overdue 标记，存在逾期时发布当日催还公告。
     * 由每日定时任务调用，管理员也可手动触发（幂等，同一天不会重复发公告）。
     */
    OverdueCheckVO checkOverdue();

    /**
     * 导出用全量记录（按 id 升序，含书名与借阅人用户名冗余字段）。
     *
     * @param status 0 在借 / 1 已还 / null 全部
     */
    List<BorrowRecord> listForExport(Integer status);
}
