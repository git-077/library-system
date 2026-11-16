package com.library.module.stat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.module.book.entity.Book;
import com.library.module.book.service.BookService;
import com.library.module.borrow.entity.BorrowRecord;
import com.library.module.borrow.service.BorrowService;
import com.library.module.category.service.CategoryService;
import com.library.module.notice.service.NoticeService;
import com.library.module.stat.dto.BorrowTrendVO;
import com.library.module.stat.dto.CategoryStatVO;
import com.library.module.stat.dto.DayCountDTO;
import com.library.module.stat.dto.StatOverviewVO;
import com.library.module.stat.dto.TopBookVO;
import com.library.module.stat.mapper.StatMapper;
import com.library.module.stat.service.StatService;
import com.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台统计实现
 */
@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserService userService;
    private final BookService bookService;
    private final BorrowService borrowService;
    private final CategoryService categoryService;
    private final NoticeService noticeService;
    private final StatMapper statMapper;

    @Override
    public StatOverviewVO overview() {
        StatOverviewVO vo = new StatOverviewVO();
        vo.setUserCount(userService.count());
        vo.setBookCount(bookService.count());
        vo.setCategoryCount(categoryService.count());
        vo.setNoticeCount(noticeService.count());
        vo.setActiveBorrowCount(borrowService.count(
                new LambdaQueryWrapper<BorrowRecord>().eq(BorrowRecord::getStatus, 0)));
        // 逾期：仍在借且应还时间已过
        vo.setOverdueCount(borrowService.count(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getStatus, 0)
                        .lt(BorrowRecord::getDueDate, LocalDateTime.now())));
        List<Book> books = bookService.list();
        vo.setTotalStock(books.stream().mapToLong(b -> b.getStock() == null ? 0 : b.getStock()).sum());
        return vo;
    }

    @Override
    public List<BorrowTrendVO> borrowTrend(int days) {
        int span = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(span - 1L);

        // SQL 只返回有数据的日期，这里先按天铺满再回填，避免折线在空日期处断裂
        Map<String, int[]> filled = new HashMap<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            filled.put(d.format(DAY), new int[]{0, 0});
        }
        for (DayCountDTO row : statMapper.countBorrowByDay(from.atStartOfDay())) {
            int[] cell = filled.get(row.getDay());
            if (cell != null) {
                cell[0] = row.getCnt();
            }
        }
        for (DayCountDTO row : statMapper.countReturnByDay(from.atStartOfDay())) {
            int[] cell = filled.get(row.getDay());
            if (cell != null) {
                cell[1] = row.getCnt();
            }
        }

        List<BorrowTrendVO> list = new ArrayList<>(span);
        LocalDate cursor = from;
        while (!cursor.isAfter(today)) {
            String key = cursor.format(DAY);
            int[] cell = filled.get(key);
            BorrowTrendVO vo = new BorrowTrendVO();
            vo.setDate(key);
            vo.setBorrowCount(cell[0]);
            vo.setReturnCount(cell[1]);
            list.add(vo);
            cursor = cursor.plusDays(1);
        }
        return list;
    }

    @Override
    public List<CategoryStatVO> categoryDistribution() {
        return statMapper.categoryDistribution();
    }

    @Override
    public List<TopBookVO> topBooks(int limit) {
        return statMapper.topBooks(Math.min(Math.max(limit, 1), 50));
    }
}
