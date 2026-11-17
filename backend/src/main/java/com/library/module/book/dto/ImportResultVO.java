package com.library.module.book.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果：统计数字给管理者看整体，issues 给出逐行原因方便直接修表重传。
 */
@Data
public class ImportResultVO {

    /** 有效数据行数（不含空行和表头） */
    private int total;

    private int success;

    /** 数据库里已存在同 ISBN，跳过（不覆盖已有馆藏与库存） */
    private int skipped;

    private int failed;

    private List<RowIssue> issues = new ArrayList<>();

    public void addIssue(int line, String isbn, String name, String result, String reason) {
        issues.add(new RowIssue(line, isbn, name, result, reason));
    }

    /**
     * @param line   文件中的行号（1 开始，与 Excel 行号一致，便于用户定位）
     * @param result SKIPPED / FAILED
     */
    public record RowIssue(int line, String isbn, String name, String result, String reason) {
    }
}
