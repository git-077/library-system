package com.library.module.book.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.module.book.dto.BookDTO;
import com.library.module.book.dto.ImportResultVO;
import com.library.module.book.entity.Book;
import com.library.module.book.service.BookImportService;
import com.library.module.book.service.BookService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 图书接口：
 * 检索对所有登录用户开放；增删改（含上下架、软删除）仅管理员。
 * <p>写接口一律接收 {@link BookDTO}，不直接暴露实体，避免前端改到服务端字段。
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookImportService bookImportService;

    /** 分页检索：keyword 模糊匹配书名/作者/ISBN */
    @GetMapping("/page")
    public Result<IPage<Book>> page(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) Integer status) {
        return Result.ok(bookService.pageBooks(pageNum, pageSize, keyword, categoryId, status));
    }

    @GetMapping("/{id}")
    public Result<Book> detail(@PathVariable Long id) {
        return Result.ok(bookService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "图书", action = "新增")
    public Result<Void> add(@Valid @RequestBody BookDTO dto) {
        bookService.addBook(dto);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "图书", action = "编辑")
    public Result<Void> update(@Valid @RequestBody BookDTO dto) {
        bookService.updateBook(dto);
        return Result.ok();
    }

    /** 上下架：status=0 下架 / 1 上架 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "图书", action = "上下架")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        bookService.changeStatus(id, status);
        return Result.ok();
    }

    /**
     * 批量导入 CSV：校验通过的行入库，坏行逐条返回原因，其余行不受影响。
     * 分类按名称匹配，库里没有的自动创建。
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "图书", action = "批量导入")
    public Result<ImportResultVO> importBooks(@RequestParam("file") MultipartFile file) {
        return Result.ok(bookImportService.importCsv(file));
    }

    /** 下载导入模板（表头 + 两行示例，UTF-8 BOM 保证 Excel 打开不乱码） */
    @GetMapping("/import-template")
    @PreAuthorize("hasRole('ADMIN')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        bookImportService.writeTemplate(response);
    }

    /** 删除图书（软删除，借阅历史不受影响） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "图书", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.removeById(id);
        return Result.ok();
    }
}
