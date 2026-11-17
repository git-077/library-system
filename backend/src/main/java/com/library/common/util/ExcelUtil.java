package com.library.common.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * Excel(.xlsx) 导出工具，EasyExcel「无实体模型」写法。
 *
 * <p>与 {@link CsvUtil#write} 对偶：调用方准备同一份 header + rows，两种格式导出的列完全一致，
 * 调用方只需按 format 参数二选一。选 EasyExcel 而不是裸 POI 的原因：
 * 注解/实体映射那套用不上（借阅导出是动态列），而裸 POI 写表头样式、列宽要十几行样板代码；
 * EasyExcel 的流式写（默认 10 万行刷一次盘）也避免大数据量撑爆堆。
 *
 * <p>版本锁 3.3.4：EasyExcel 3.4+ 要求 Java 21，本项目运行在 Java 17。
 */
public final class ExcelUtil {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** 与 Excel 单元格类型对应的 MIME，让浏览器/Office 识别为 xlsx 而不是 zip */
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ExcelUtil() {
    }

    /**
     * 写到 HTTP 响应（触发浏览器下载）。文件名形如 borrows-20261117-103700.xlsx。
     *
     * @param fileNamePrefix 文件名前缀，也是默认工作表名
     */
    public static void write(HttpServletResponse response, String fileNamePrefix,
                             List<String> header, List<? extends List<?>> rows) throws IOException {
        String fileName = fileNamePrefix + "-" + LocalDateTime.now().format(STAMP) + ".xlsx";
        response.reset();
        response.setContentType(XLSX_MIME);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 与 CsvUtil 相同的 RFC 5987 双写法，兼容中文文件名与各浏览器
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''"
                        + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        writeTo(response.getOutputStream(), fileNamePrefix, header, rows);
    }

    /**
     * 核心写法，不依赖 Servlet 便于单测：head 是 List&lt;List&lt;String&gt;&gt;
     * （外层每项=一列，内层=多级表头，这里只有一级），数据每行一个 List。
     *
     * <p>列宽用自适应策略：借阅导出里「图书名称」与「ID」宽度差几倍，
     * 固定列宽要么挤成竖排省略号、要么一半是空白。
     */
    public static void writeTo(OutputStream out, String sheetName,
                               List<String> header, List<? extends List<?>> rows) {
        List<List<String>> head = header.stream()
                .map(Collections::singletonList)
                .toList();
        EasyExcel.write(out)
                .head(head)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(sanitize(sheetName))
                .doWrite(rows);
    }

    /**
     * Excel 工作表名不允许出现 : \ / ? * [ ]，长度上限 31 字符。
     * 违反时 Excel 会静默改名（Sheet1），下游按名字找表的脚本就会失效——这里显式处理。
     */
    private static String sanitize(String name) {
        String cleaned = name.replaceAll("[:\\\\/?*\\[\\]]", "-");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
