package com.library.module.stat.service;

import com.library.module.stat.dto.BorrowTrendVO;
import com.library.module.stat.dto.CategoryStatVO;
import com.library.module.stat.dto.StatOverviewVO;
import com.library.module.stat.dto.TopBookVO;

import java.util.List;

/**
 * 后台统计服务（仅管理员调用）。
 */
public interface StatService {

    /** 管理后台首页概览指标 */
    StatOverviewVO overview();

    /**
     * 近 N 天借还趋势（无数据的日期补 0，保证折线不出断点）。
     *
     * @param days 统计天数，会被收敛到 1~90
     */
    List<BorrowTrendVO> borrowTrend(int days);

    /** 分类维度馆藏分布（含零馆藏分类） */
    List<CategoryStatVO> categoryDistribution();

    /** 借阅次数排行 */
    List<TopBookVO> topBooks(int limit);
}
