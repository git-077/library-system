package com.library.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 自助修改密码入参。
 * <p>上限 32 与注册保持一致；下限 6 位是底线要求，不强制复杂度是为了避免
 * 「必须大小写+符号」把密码逼成便利贴上的便签。
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "请输入原密码")
    @Size(max = 32, message = "原密码长度不合法")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位")
    private String newPassword;
}
