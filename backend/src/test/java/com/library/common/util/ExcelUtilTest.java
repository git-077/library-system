package com.library.common.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExcelUtil 的行为锚点：不依赖 Spring 上下文，
 * 直接把 EasyExcel 写出的字节流用 POI 读回来断言——
 * 「写出去的东西能被标准读回」比断言内部实现更可信。
 */
class ExcelUtilTest {

    private static final List<String> HEADER =
            List.of("记录ID", "借阅人", "书名", "状态");

    private static final List<List<Object>> ROWS = List.of(
            List.of(1L, "reader", "三体", "借出中"),
            List.of(2L, "admin", "《百年孤独》，含逗号与中文书名号", "已归还"));

    /** 写入内存并读回第一个工作表 */
    private XSSFWorkbook writeAndRead(List<List<Object>> rows, String sheetName) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelUtil.writeTo(bos, sheetName, HEADER, rows);
        return new XSSFWorkbook(new ByteArrayInputStream(bos.toByteArray()));
    }

    @Test
    @DisplayName("表头与数据逐格读回一致，中文不乱码")
    void headerAndRowsRoundTrip() throws IOException {
        try (XSSFWorkbook wb = writeAndRead(ROWS, "borrows")) {
            assertThat(wb.getSheet("borrows")).as("按名字能找到工作表").isNotNull();

            Row head = wb.getSheet("borrows").getRow(0);
            for (int i = 0; i < HEADER.size(); i++) {
                assertThat(head.getCell(i).getStringCellValue()).isEqualTo(HEADER.get(i));
            }

            Row first = wb.getSheet("borrows").getRow(1);
            assertThat(first.getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(first.getCell(1).getStringCellValue()).isEqualTo("reader");
            // 0.5 行内数字列（ID/续借次数）写的是 Number 类型，Excel 里可直接求和
            assertThat(first.getCell(0).getCellType()).isEqualTo(CellType.NUMERIC);

            Row second = wb.getSheet("borrows").getRow(2);
            assertThat(second.getCell(2).getStringCellValue()).contains("，含逗号");
        }
    }

    @Test
    @DisplayName("空数据也能产出合法文件：只有表头，Excel 打开不报错")
    void emptyRowsStillValid() throws IOException {
        try (XSSFWorkbook wb = writeAndRead(List.of(), "borrows")) {
            Row head = wb.getSheet("borrows").getRow(0);
            assertThat(head.getCell(0).getStringCellValue()).isEqualTo("记录ID");
            // Row 实现了 Iterable<Cell>，直接传会让 assertThat(T) 与 assertThat(Iterable) 二义，先转 Object
            assertThat((Object) wb.getSheet("borrows").getRow(1)).as("数据区为空").isNull();
        }
    }

    @Test
    @DisplayName("工作表名非法字符被替换、超长被截断（Excel 会静默改名，工具必须先行处理）")
    void sheetNameSanitized() throws IOException {
        String bad = "a/b\\c:d?e*f[g]h超过三十一个字符的超级长的工作表名称需要被截断";
        try (XSSFWorkbook wb = writeAndRead(ROWS, bad)) {
            assertThat(wb.getSheetAt(0).getSheetName())
                    .doesNotContain("/", "\\", ":", "?", "*", "[", "]")
                    .hasSizeLessThanOrEqualTo(31);
        }
    }

    @Test
    @DisplayName("1000 行流式写出：行数正确、内存压力可控（EasyExcel 默认分批刷盘）")
    void largeRowsRoundTrip() throws IOException {
        List<List<Object>> rows = new java.util.ArrayList<>(1000);
        for (long i = 1; i <= 1000; i++) {
            rows.add(List.of(i, "reader-" + i, "书名-" + i, "借出中"));
        }
        try (XSSFWorkbook wb = writeAndRead(rows, "borrows")) {
            assertThat(wb.getSheet("borrows").getLastRowNum()).isEqualTo(1000);
            Row last = wb.getSheet("borrows").getRow(1000);
            Cell idCell = last.getCell(0);
            assertThat(idCell.getNumericCellValue()).isEqualTo(1000.0);
        }
    }
}
