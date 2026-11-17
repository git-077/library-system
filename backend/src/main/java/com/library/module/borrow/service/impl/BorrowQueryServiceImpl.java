package com.library.module.borrow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.mapper.BorrowMapper;
import com.library.module.borrow.service.BorrowQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BorrowQueryServiceImpl implements BorrowQueryService {

    private final BorrowMapper borrowMapper;

    @Override
    public long countActive(Long userId) {
        return borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, 0));
    }

    @Override
    public long countOverdue(Long userId) {
        // 逾期一律按 due_date 实时判断，不依赖定时任务刷出来的 overdue 标记：
        // 任务一天只跑一次，夜里到期的书不能等到第二天才算逾期
        return borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, 0)
                .lt(BorrowRecord::getDueDate, LocalDateTime.now()));
    }

    @Override
    public boolean hasActiveBorrow(Long userId, Long bookId) {
        return borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getBookId, bookId)
                .eq(BorrowRecord::getStatus, 0)) > 0;
    }
}
