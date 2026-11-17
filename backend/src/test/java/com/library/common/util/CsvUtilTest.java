package com.library.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV 解析与转义的边界用例。
 * 这些场景在真实书单里都会遇到：书名带逗号、简介带换行、Excel 导出的 BOM。
 */
class CsvUtilTest {

    private List<List<String>> parse(String text) throws IOException {
        return CsvUtil.parse(new StringReader(text));
    }

    @Test
    @DisplayName("基础解析：表头 + 数据行，忽略尾部换行")
    void parseSimple() throws IOException {
        List<List<String>> rows = parse("isbn,name\n111,A\n222,B\n");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).containsExactly("isbn", "name");
        assertThat(rows.get(2)).containsExactly("222", "B");
    }

    @Test
    @DisplayName("引号包裹的字段可包含逗号，不会被拆列")
    void parseQuotedComma() throws IOException {
        List<List<String>> rows = parse("isbn,name\n111,\"重构：改善既有代码的设计, 第2版\"\n");
        assertThat(rows.get(1)).containsExactly("111", "重构：改善既有代码的设计, 第2版");
    }

    @Test
    @DisplayName("引号内的 \"\" 还原为一个双引号")
    void parseEscapedQuote() throws IOException {
        List<List<String>> rows = parse("a\n\"他说\"\"你好\"\"\"\n");
        assertThat(rows.get(1)).containsExactly("他说\"你好\"");
    }

    @Test
    @DisplayName("引号内的换行属于同一单元格，不拆行")
    void parseQuotedNewline() throws IOException {
        List<List<String>> rows = parse("isbn,desc\n111,\"第一行\n第二行\"\n");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).get(1)).isEqualTo("第一行\n第二行");
    }

    @Test
    @DisplayName("剥掉 Excel 导出的 UTF-8 BOM，否则第一列表头会认不出来")
    void parseStripBom() throws IOException {
        List<List<String>> rows = parse("\uFEFFisbn,name\n111,A\n");
        assertThat(rows.get(0).get(0)).isEqualTo("isbn");
    }

    @Test
    @DisplayName("CRLF / CR / LF 三种换行混用都能正确拆行")
    void parseMixedLineEndings() throws IOException {
        List<List<String>> rows = parse("a,b\r\n1,2\r3,4\n5,6");
        assertThat(rows).hasSize(4);
        assertThat(rows.get(2)).containsExactly("3", "4");
        assertThat(rows.get(3)).containsExactly("5", "6");
    }

    @Test
    @DisplayName("跳过空白行，不产生只有空串的行")
    void parseSkipBlankLines() throws IOException {
        List<List<String>> rows = parse("a,b\n\n1,2\n\n");
        assertThat(rows).hasSize(2);
    }

    @Test
    @DisplayName("escape 与 parse 互逆：带逗号、引号、换行的值导出后能原样读回")
    void escapeRoundTrip() throws IOException {
        String nasty = "书名,含逗号 和 \"引号\"\n还有换行";
        String line = CsvUtil.escape(nasty) + "\n";
        List<List<String>> rows = parse(line);
        assertThat(rows.get(0)).containsExactly(nasty);
    }
}
