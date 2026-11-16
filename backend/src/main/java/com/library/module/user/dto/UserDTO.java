package com.library.module.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员新增 / 编辑用户入参。
 * <p>password 仅新增时必填，编辑时留空表示不改密码（改密码走重置接口）。
 */
@Data
public class UserDTO {

    @Size(min = 3, max = 20, message = "用户名长度需在 3~20 位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @Size(min = 6, max = 20, message = "密码长度需在 6~20 位之间")
    private String password;

    @Size(max = 50, message = "真实姓名过长")
    private String realName;

    /** admin / user，为空时默认 user */
    private String role;

    /** 0禁用 1启用 */
    private Integer status;
}
