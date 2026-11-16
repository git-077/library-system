package com.library.module.portal.controller;

import com.library.common.result.Result;
import com.library.module.portal.dto.PortalOverviewVO;
import com.library.module.portal.service.PortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者门户的公开概览数据。
 * 与 /api/stat/overview（仅管理员）区分开：这里的数字用于门户首页展示，
 * 只统计已上架图书的聚合值，不含任何敏感信息，登录用户均可访问。
 */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalController {

    private final PortalService portalService;

    @GetMapping("/overview")
    public Result<PortalOverviewVO> overview() {
        return Result.ok(portalService.overview());
    }
}
