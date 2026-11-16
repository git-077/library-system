package com.library.common.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 极简 CSV 导出工具（零第三方依赖，避免为一个导出功能引入 EasyExcel/POI）。
 *
 * <p>两个易踩的坑都处理了：
 * <ul>
 *   <li><b>Excel 中文乱码</b>：文件头写入 UTF-8 BOM，双击打开才不会乱码；</li>
 *   <li><b>字段转义</b>：含逗号 / 双引号 / 换行的内容必须用双引号包裹，内部引号翻倍，
 *       否则列会错位（书名里带逗号很常见，例如「重构：改善既有代码的设计」）。</li>
 * </ul>
 */
public final class CsvUtil {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private CsvUtil() {
    }

    /**
     * 把表头 + 数据行写进响应流。
     *
     * @param fileNamePrefix 文件名前缀，最终文件名形如 borrows-20261116-134500.csv
     */
    public static void write(HttpServletResponse response, String fileNamePrefix,
                             List<String> header, List<List<?>> rows) throws IOException {
        String fileName = fileNamePrefix + "-" + LocalDateTime.now().format(STAMP) + ".csv";
        response.reset();
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // RFC 5987 写法，兼容中文文件名与各浏览器
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''"
                        + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        appendLine(sb, header);
        for (List<?> row : rows) {
            appendLine(sb, row);
        }

        try (OutputStream out = response.getOutputStream()) {
            out.write(UTF8_BOM);
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private static void appendLine(StringBuilder sb, List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells.get(i)));
        }
        // Excel 对 \r\n 兼容最好
        sb.append("\r\n");
    }

    /** 转义：含分隔符或引号时整体加引号，内部引号翻倍 */
    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.indexOf(',') < 0 && text.indexOf('"') < 0
                && text.indexOf('\n') < 0 && text.indexOf('\r') < 0) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
