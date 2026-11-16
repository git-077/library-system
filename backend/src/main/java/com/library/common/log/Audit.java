package com.library.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计标记：加在 Controller 写接口上，由切面自动记录到 operation_log。
 *
 * <p>只做「标记」，具体落库逻辑在 module.log.aspect 中，
 * 这样 common 层不反向依赖业务模块。
 *
 * <pre>
 * &#64;Audit(module = "图书", action = "新增")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 业务模块：图书 / 分类 / 公告 / 用户 / 借阅 */
    String module();

    /** 动作描述：新增 / 编辑 / 删除 / 上下架 ... */
    String action();

    /** 是否记录入参快照；涉及大字段或敏感数据时可关掉 */
    boolean saveParams() default true;
}
