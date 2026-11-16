package com.library.module.stat.controller;

import com.library.common.result.Result;
import com.library.module.stat.dto.BorrowTrendVO;
import com.library.module.stat.dto.CategoryStatVO;
import com.library.module.stat.dto.StatOverviewVO;
import com.library.module.stat.dto.TopBookVO;
import com.library.module.stat.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计接口（仅管理员）
 */
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StatController {

    private final StatService statService;

    /** 概览指标卡片 */
    @GetMapping("/overview")
    public Result<StatOverviewVO> overview() {
        return Result.ok(statService.overview());
    }

    /** 近 N 天借还趋势（默认 30 天） */
    @GetMapping("/borrow-trend")
    public Result<List<BorrowTrendVO>> borrowTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statService.borrowTrend(days));
    }

    /** 分类馆藏分布 */
    @GetMapping("/category-distribution")
    public Result<List<CategoryStatVO>> categoryDistribution() {
        return Result.ok(statService.categoryDistribution());
    }

    /** 借阅次数排行（默认前 10） */
    @GetMapping("/top-books")
    public Result<List<TopBookVO>> topBooks(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statService.topBooks(limit));
    }
}
