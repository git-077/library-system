package com.library.module.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.library.module.log.entity.OperationLog;
import com.library.module.log.mapper.OperationLogMapper;
import com.library.module.log.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志实现。
 */
@Slf4j
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    @Override
    public void record(OperationLog entry) {
        try {
            save(entry);
        } catch (Exception e) {
            // 审计日志是旁路能力：写失败只告警，不能让业务请求失败
            log.warn("操作日志写入失败 module={} action={}：{}",
                    entry.getModule(), entry.getAction(), e.getMessage());
        }
    }

    @Override
    public IPage<OperationLog> pageLogs(long pageNum, long pageSize, String keyword,
                                        String module, Integer status) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(OperationLog::getUsername, kw).or().like(OperationLog::getUri, kw));
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (status != null) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        wrapper.orderByDesc(OperationLog::getCreateTime).orderByDesc(OperationLog::getId);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
