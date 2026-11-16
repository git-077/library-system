package com.library.module.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告新增 / 编辑入参。
 * <p>长度与 sql/init.sql 的 notice 表列定义保持一致（content 为 TEXT）。
 */
@Data
public class NoticeDTO {

    /** notice.title VARCHAR(200) */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "标题不能超过 200 个字符")
    private String title;

    /** notice.content TEXT */
    @NotBlank(message = "公告内容不能为空")
    @Size(max = 5000, message = "内容不能超过 5000 个字符")
    private String content;
}
