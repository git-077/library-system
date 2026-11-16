package com.library.module.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（审计记录，只增不改）。
 */
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID（登录态丢失时可能为空） */
    private Long userId;

    /** 操作人用户名快照：用户改名/删号后日志仍可读 */
    private String username;

    /** 业务模块：图书 / 分类 / 公告 / 用户 / 借阅 */
    private String module;

    /** 动作：新增 / 编辑 / 删除 / 上下架 / 借出 / 归还 / 续借 ... */
    private String action;

    private String httpMethod;

    private String uri;

    /** 入参 JSON 快照（敏感字段脱敏、超长截断） */
    private String params;

    private String ip;

    /** 1成功 0失败 */
    private Integer status;

    private String errorMsg;

    /** 业务方法耗时（毫秒） */
    private Integer costMs;

    private LocalDateTime createTime;
}
