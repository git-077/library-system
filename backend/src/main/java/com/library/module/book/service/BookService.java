package com.library.module.book.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.entity.Book;

public interface BookService extends IService<Book> {

    /** 分页检索图书（书名 / 作者 / ISBN 模糊 + 分类过滤），并填充分类名 */
    IPage<Book> pageBooks(long pageNum, long pageSize, String keyword, Long categoryId, Integer status);

    /** 填充分类名称（避免前端二次请求） */
    void fillCategoryName(IPage<Book> page);

    /** 新增图书（校验 ISBN 唯一，库存与借出数由服务端初始化） */
    void addBook(BookDTO dto);

    /** 编辑图书（校验 ISBN 唯一，库存字段不受理） */
    void updateBook(BookDTO dto);

    /** 上下架：status=0 下架 / 1 上架 */
    void changeStatus(Long id, Integer status);
}
