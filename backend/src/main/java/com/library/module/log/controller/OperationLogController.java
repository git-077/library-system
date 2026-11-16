package com.library.module.log.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.library.common.result.Result;
import com.library.module.log.entity.OperationLog;
import com.library.module.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询（仅管理员）。
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/page")
    public Result<IPage<OperationLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String module,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(operationLogService.pageLogs(pageNum, pageSize, keyword, module, status));
    }
}
