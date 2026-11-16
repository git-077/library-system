package com.library.module.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.module.user.dto.UserDTO;
import com.library.module.user.entity.User;
import com.library.module.user.service.UserService;
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

/**
 * 用户管理接口（仅管理员）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    /** 分页查询用户，支持按用户名模糊搜索 */
    @GetMapping("/page")
    public Result<IPage<User>> page(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String username) {
        return Result.ok(userService.pageUsers(pageNum, pageSize, username));
    }

    /** 新增用户（管理员创建，角色不传时默认读者） */
    @PostMapping
    @Audit(module = "用户", action = "新增")
    public Result<Void> add(@Valid @RequestBody UserDTO dto) {
        userService.createUser(dto.getUsername(), dto.getPassword(), dto.getRealName(), dto.getRole());
        return Result.ok();
    }

    /** 编辑用户基础信息（真实姓名 / 角色 / 状态） */
    @PutMapping("/{id}")
    @Audit(module = "用户", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.updateBaseInfo(id, dto.getRealName(), dto.getRole(), dto.getStatus());
        return Result.ok();
    }

    /** 启用 / 禁用账户 */
    @PutMapping("/{id}/status")
    @Audit(module = "用户", action = "状态变更")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateBaseInfo(id, null, null, status);
        return Result.ok();
    }

    /** 重置密码为 123456 */
    @PutMapping("/{id}/password")
    @Audit(module = "用户", action = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    /** 删除用户（软删除） */
    @DeleteMapping("/{id}")
    @Audit(module = "用户", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.ok();
    }
}
