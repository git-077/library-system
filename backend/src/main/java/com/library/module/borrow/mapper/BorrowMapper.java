package com.library.module.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.module.borrow.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface BorrowMapper extends BaseMapper<BorrowRecord> {

    /**
     * 把「在借且已过应还时间」的记录标为逾期。
     * 只更新 overdue = 0 的行，重复执行不会产生无意义的写放大。
     */
    @Update("UPDATE borrow_record SET overdue = 1 " +
            "WHERE status = 0 AND overdue = 0 AND due_date < #{now}")
    int markOverdue(@Param("now") LocalDateTime now);

    /**
     * 反向修复：续借后应还时间后移，原本被标记逾期的记录要恢复为正常。
     */
    @Update("UPDATE borrow_record SET overdue = 0 " +
            "WHERE status = 0 AND overdue = 1 AND due_date >= #{now}")
    int clearOverdue(@Param("now") LocalDateTime now);
}
