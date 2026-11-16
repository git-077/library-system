package com.library.module.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.common.exception.BizException;
import com.library.module.category.dto.CategoryDTO;
import com.library.module.category.entity.Category;
import com.library.module.category.mapper.CategoryMapper;
import com.library.module.category.service.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public void createCategory(CategoryDTO dto) {
        checkNameUnique(dto.getName(), null);
        Category category = new Category();
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        save(category);
    }

    @Override
    public void updateCategory(Long id, CategoryDTO dto) {
        if (getById(id) == null) {
            throw new BizException("分类不存在");
        }
        checkNameUnique(dto.getName(), id);
        Category category = new Category();
        category.setId(id);
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        updateById(category);
    }

    /** 名称唯一校验：excludeId 用于编辑时排除自身 */
    private void checkNameUnique(String name, Long excludeId) {
        if (name == null || name.isBlank()) {
            throw new BizException("分类名称不能为空");
        }
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(Category::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BizException("分类名称已存在");
        }
    }
}
