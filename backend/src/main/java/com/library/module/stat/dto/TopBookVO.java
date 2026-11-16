package com.library.module.stat.dto;

import lombok.Data;

/**
 * 热门图书排行（借阅次数降序）。
 */
@Data
public class TopBookVO {

    private Long bookId;

    private String bookName;

    private String author;

    private String categoryName;

    /** 历史累计借阅次数（含已归还，统计的是流行度而非在借量） */
    private int borrowCount;
}
