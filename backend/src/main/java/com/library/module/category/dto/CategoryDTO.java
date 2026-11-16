package com.library.module.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 图书分类新增 / 编辑入参。
 * <p>长度与 sql/init.sql 的 category 表列定义保持一致。
 */
@Data
public class CategoryDTO {

    /** category.name VARCHAR(50) */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 个字符")
    private String name;

    /** category.description VARCHAR(200) */
    @Size(max = 200, message = "分类描述不能超过 200 个字符")
    private String description;
}
