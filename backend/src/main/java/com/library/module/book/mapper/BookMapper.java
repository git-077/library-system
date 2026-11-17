package com.library.module.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.module.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 条件扣减库存：仅当在馆库存 > 0 且图书上架时才执行。
     * 并发借阅下「先查后改」会超借，此写法以受影响行数判断成败：
     * 返回 0 即库存不足或已被并发借走，由 Service 层抛异常触发事务回滚。
     */
    @Update("UPDATE book SET stock = stock - 1, borrowed = borrowed + 1 " +
            "WHERE id = #{bookId} AND stock > 0 AND status = 1 AND deleted = 0")
    int deductStock(@Param("bookId") Long bookId);

    /** 归还时回写库存（逆向操作） */
    @Update("UPDATE book SET stock = stock + 1, borrowed = borrowed - 1 " +
            "WHERE id = #{bookId} AND borrowed > 0 AND deleted = 0")
    int returnStock(@Param("bookId") Long bookId);

    /**
     * 把刚归还的副本<strong>锁定</strong>给排队的第一位读者：从可借库存移到 held。
     * <p>条件 {@code stock > 0} 保证并发归还/借出时不会把库存扣成负数；
     * 返回 0 表示副本已被别人借走，此时调用方应放弃这次「让位」，把预约留给下次归还。
     */
    @Update("UPDATE book SET stock = stock - 1, held = held + 1 " +
            "WHERE id = #{bookId} AND stock > 0 AND deleted = 0")
    int holdCopy(@Param("bookId") Long bookId);

    /** 释放锁定（预约取消 / 过期）：副本回到可借库存 */
    @Update("UPDATE book SET stock = stock + 1, held = held - 1 " +
            "WHERE id = #{bookId} AND held > 0 AND deleted = 0")
    int releaseHold(@Param("bookId") Long bookId);

    /**
     * 持「待取书」预约的读者来借书：消耗锁定给他的那份副本。
     * <p>不走 {@link #deductStock}，因为那份副本在放行时已经离开了 stock，
     * 此时应做的是 {@code held - 1, borrowed + 1}。
     */
    @Update("UPDATE book SET held = held - 1, borrowed = borrowed + 1 " +
            "WHERE id = #{bookId} AND held > 0 AND deleted = 0")
    int lendHeld(@Param("bookId") Long bookId);

    /**
     * 取全部 ISBN（<b>含已软删除的书</b>）。
     *
     * <p>批量导入前要用它做去重判断：{@code book.isbn} 上有唯一索引 {@code uk_isbn}，
     * 而 MyBatis-Plus 的逻辑删除只会过滤查询结果、不会放开索引约束。
     * 如果按「未删除」判断去重，遇到已删除的同 ISBN 书仍会撞唯一键，
     * 让整批导入失败且错误信息难以理解。这里用原生 SQL 绕过逻辑删除插件。
     */
    @Select("SELECT isbn FROM book")
    List<String> selectAllIsbn();
}
