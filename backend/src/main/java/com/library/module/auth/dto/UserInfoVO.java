package com.library.module.auth.dto;

import lombok.Data;

/**
 * 当前登录用户信息出参。
 * <p>刻意不返回 password / deleted 等字段，避免把实体直接暴露给前端。
 */
@Data
public class UserInfoVO {

    private Long id;

    private String username;

    private String realName;

    private String role;

    private Integer status;
}
