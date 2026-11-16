package com.library.module.category.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.category.dto.CategoryDTO;
import com.library.module.category.entity.Category;

public interface CategoryService extends IService<Category> {

    /** 新增分类（名称不可重复） */
    void createCategory(CategoryDTO dto);

    /** 编辑分类（名称不可与其他分类重复） */
    void updateCategory(Long id, CategoryDTO dto);
}
