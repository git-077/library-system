package com.library.module.category.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.module.category.dto.CategoryDTO;
import com.library.module.category.entity.Category;
import com.library.module.category.service.CategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口：查询对所有登录用户开放，增删改仅管理员
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(categoryService.list(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "分类", action = "新增")
    public Result<Void> add(@Valid @RequestBody CategoryDTO dto) {
        categoryService.createCategory(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "分类", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        categoryService.updateCategory(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "分类", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.ok();
    }
}
