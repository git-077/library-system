package com.library.module.portal.dto;

import lombok.Data;

/**
 * 读者门户概览出参（首页展示用，不含任何敏感信息）。
 * <p>用 VO 替代原来的 Map&lt;String,Object&gt;，字段名即契约，改动时编译期就能发现。
 */
@Data
public class PortalOverviewVO {

    /** 上架品种数 */
    private long bookCount;

    /** 在馆册数 */
    private long stockTotal;

    /** 已借出册数 */
    private long borrowedTotal;

    /** 分类数 */
    private long categoryCount;

    /** 读者人数 */
    private long readerCount;
}
