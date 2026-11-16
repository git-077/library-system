package com.library.module.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.common.exception.BizException;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.book.service.BookService;
import com.library.module.category.entity.Category;
import com.library.module.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 图书服务实现
 */
@Service
@RequiredArgsConstructor
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    private final CategoryService categoryService;

    @Override
    public IPage<Book> pageBooks(long pageNum, long pageSize, String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Book::getName, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getIsbn, keyword))
                .eq(categoryId != null, Book::getCategoryId, categoryId)
                .eq(status != null, Book::getStatus, status)
                .orderByDesc(Book::getId);
        IPage<Book> page = page(new Page<>(pageNum, pageSize), wrapper);
        fillCategoryName(page);
        return page;
    }

    @Override
    public void fillCategoryName(IPage<Book> page) {
        fillCategoryName(page.getRecords());
    }

    private void fillCategoryName(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }
        List<Long> categoryIds = books.stream()
                .map(Book::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        books.forEach(book -> book.setCategoryName(nameMap.get(book.getCategoryId())));
    }

    @Override
    public void addBook(BookDTO dto) {
        checkIsbnUnique(dto.getIsbn(), null);
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        if (book.getStock() == null || book.getStock() < 0) {
            throw new BizException("库存数量不合法");
        }
        // id / 已借出 / 删除标记 / 上架状态都由服务端决定，不接受前端指定
        book.setId(null);
        book.setBorrowed(0);
        book.setDeleted(0);
        book.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        save(book);
    }

    @Override
    public void updateBook(BookDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("图书 id 不能为空");
        }
        Book db = getById(dto.getId());
        if (db == null) {
            throw new BizException("图书不存在");
        }
        checkIsbnUnique(dto.getIsbn(), dto.getId());

        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        // 库存与已借出数量由借阅事务维护，编辑接口不允许直接改
        book.setStock(db.getStock());
        book.setBorrowed(db.getBorrowed());
        if (book.getStatus() == null) {
            book.setStatus(db.getStatus());
        }
        updateById(book);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态值不合法，只能是 0（下架）或 1（上架）");
        }
        if (getById(id) == null) {
            throw new BizException("图书不存在");
        }
        Book book = new Book();
        book.setId(id);
        book.setStatus(status);
        updateById(book);
    }

    /** ISBN 唯一校验：excludeId 用于编辑时排除自身 */
    private void checkIsbnUnique(String isbn, Long excludeId) {
        if (isbn == null || isbn.isBlank()) {
            throw new BizException("ISBN 不能为空");
        }
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<Book>().eq(Book::getIsbn, isbn);
        if (excludeId != null) {
            wrapper.ne(Book::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BizException("ISBN 已存在，请勿重复录入");
        }
    }
}
