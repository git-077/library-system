package com.library.module.log.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.library.module.log.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {

    /** 写入一条审计日志（内部吞异常，绝不因日志失败影响业务） */
    void record(OperationLog log);

    /**
     * 日志分页查询。
     *
     * @param keyword 模糊匹配操作人用户名 / 请求路径
     * @param module  模块精确筛选，空表示全部
     * @param status  1成功 0失败，null 表示全部
     */
    IPage<OperationLog> pageLogs(long pageNum, long pageSize, String keyword, String module, Integer status);
}
