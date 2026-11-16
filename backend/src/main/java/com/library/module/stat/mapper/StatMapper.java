package com.library.module.stat.mapper;

import com.library.module.stat.dto.CategoryStatVO;
import com.library.module.stat.dto.DayCountDTO;
import com.library.module.stat.dto.TopBookVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统计专用查询（只读，不走实体 CRUD）。
 *
 * <p>放在 stat 模块而非各业务模块，是为了让「报表明细口径」集中在一处，
 * 避免 BookMapper / BorrowMapper 被统计 SQL 污染。
 */
@Mapper
public interface StatMapper {

    /** 按天统计借出量 */
    @Select("SELECT DATE_FORMAT(borrow_date, '%Y-%m-%d') AS day, COUNT(*) AS cnt " +
            "FROM borrow_record WHERE borrow_date >= #{start} " +
            "GROUP BY DATE(borrow_date)")
    List<DayCountDTO> countBorrowByDay(@Param("start") LocalDateTime start);

    /** 按天统计归还量（以 return_date 为准，含逾期归还） */
    @Select("SELECT DATE_FORMAT(return_date, '%Y-%m-%d') AS day, COUNT(*) AS cnt " +
            "FROM borrow_record WHERE return_date IS NOT NULL AND return_date >= #{start} " +
            "GROUP BY DATE(return_date)")
    List<DayCountDTO> countReturnByDay(@Param("start") LocalDateTime start);

    /**
     * 分类维度馆藏分布。
     * LEFT JOIN 保证没有书的分类也出现在结果里（值为 0），饼图不会漏项。
     */
    @Select("SELECT c.id AS categoryId, c.name AS categoryName, " +
            "       COUNT(b.id) AS bookCount, " +
            "       IFNULL(SUM(b.stock), 0) AS stock, " +
            "       IFNULL(SUM(b.borrowed), 0) AS borrowed " +
            "FROM category c " +
            "LEFT JOIN book b ON b.category_id = c.id AND b.deleted = 0 " +
            "GROUP BY c.id, c.name ORDER BY c.id")
    List<CategoryStatVO> categoryDistribution();

    /** 借阅次数排行（只统计未被删除/下架下线的书仍可计入历史借阅） */
    @Select("SELECT b.id AS bookId, b.name AS bookName, b.author AS author, " +
            "       c.name AS categoryName, COUNT(r.id) AS borrowCount " +
            "FROM borrow_record r " +
            "JOIN book b ON b.id = r.book_id " +
            "LEFT JOIN category c ON c.id = b.category_id " +
            "GROUP BY b.id, b.name, b.author, c.name " +
            "ORDER BY borrowCount DESC, b.id ASC LIMIT #{limit}")
    List<TopBookVO> topBooks(@Param("limit") int limit);
}
