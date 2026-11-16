package com.library.module.portal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.constant.RoleConstants;
import com.library.module.book.entity.Book;
import com.library.module.book.service.BookService;
import com.library.module.category.service.CategoryService;
import com.library.module.portal.dto.PortalOverviewVO;
import com.library.module.portal.service.PortalService;
import com.library.module.user.entity.User;
import com.library.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门户概览实现：统计口径只含「已上架」图书
 */
@Service
@RequiredArgsConstructor
public class PortalServiceImpl implements PortalService {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final UserService userService;

    @Override
    public PortalOverviewVO overview() {
        List<Book> onShelf = bookService.list(new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1));

        long stockTotal = onShelf.stream().mapToLong(b -> b.getStock() == null ? 0 : b.getStock()).sum();
        long borrowedTotal = onShelf.stream().mapToLong(b -> b.getBorrowed() == null ? 0 : b.getBorrowed()).sum();

        PortalOverviewVO vo = new PortalOverviewVO();
        vo.setBookCount(onShelf.size());
        vo.setStockTotal(stockTotal);
        vo.setBorrowedTotal(borrowedTotal);
        vo.setCategoryCount(categoryService.count());
        vo.setReaderCount(userService.count(
                new LambdaQueryWrapper<User>().eq(User::getRole, RoleConstants.READER)));
        return vo;
    }
}
