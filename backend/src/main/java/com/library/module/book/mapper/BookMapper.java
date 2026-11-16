package com.library.module.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.module.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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
}
