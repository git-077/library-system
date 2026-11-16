package com.library.module.notice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.log.Audit;
import com.library.common.result.Result;
import com.library.module.notice.dto.NoticeDTO;
import com.library.module.notice.entity.Notice;
import com.library.module.notice.service.NoticeService;
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
 * 公告接口：查询对所有登录用户开放，管理仅管理员
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/page")
    public Result<IPage<Notice>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(noticeService.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "公告", action = "新增")
    public Result<Void> add(@Valid @RequestBody NoticeDTO dto) {
        noticeService.saveNotice(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "公告", action = "编辑")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeDTO dto) {
        noticeService.updateNotice(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Audit(module = "公告", action = "删除")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.removeById(id);
        return Result.ok();
    }
}
