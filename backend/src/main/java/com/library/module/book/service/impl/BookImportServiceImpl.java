package com.library.module.book.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.common.exception.BizException;
import com.library.common.util.CsvUtil;
import com.library.module.book.dto.ImportResultVO;
import com.library.module.book.entity.Book;
import com.library.module.book.mapper.BookMapper;
import com.library.module.book.service.BookImportService;
import com.library.module.book.service.BookService;
import com.library.module.category.entity.Category;
import com.library.module.category.service.CategoryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CSV 批量导入实现。
 *
 * <p>设计上坚持三条：
 * <ul>
 *   <li><b>部分成功</b>：一行有问题不能拖累整批，坏行记原因、好行照常入库；</li>
 *   <li><b>不静默覆盖</b>：ISBN 已存在一律跳过而不是 update，避免一次误传把在馆库存和借阅关系冲掉；</li>
 *   <li><b>失败信息可操作</b>：报行号 + ISBN + 原因，管理员照着改完重传即可。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookImportServiceImpl implements BookImportService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final int MAX_ROWS = 2000;
    private static final String DEFAULT_AUTHOR = "佚名";

    private static final List<String> HEADER = List.of(
            "isbn", "name", "author", "publisher", "category", "price", "stock", "description");

    /** 表头别名 → 规范字段。大小写不敏感，中文表头也认，避免用户被模板格式卡住 */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();

    static {
        alias("isbn", "isbn", "图书isbn", "书号", "isbn号");
        alias("name", "name", "书名", "图书名称", "名称", "题名");
        alias("author", "author", "作者", "著者");
        alias("publisher", "publisher", "出版社", "出版单位");
        alias("category", "category", "分类", "类别", "分类名称");
        alias("price", "price", "定价", "价格", "单价");
        alias("stock", "stock", "库存", "数量", "在馆数量", "册数");
        alias("description", "description", "简介", "介绍", "内容简介");
    }

    private static void alias(String field, String... names) {
        for (String name : names) {
            HEADER_ALIAS.put(name, field);
        }
    }

    private final BookService bookService;
    private final BookMapper bookMapper;
    private final CategoryService categoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importCsv(MultipartFile file) {
        checkFile(file);
        List<List<String>> rows = readRows(file);
        if (rows.size() < 2) {
            throw new BizException("文件里没有数据行，请按模板填写后重新上传");
        }

        Map<String, Integer> columns = resolveHeader(rows.get(0));
        if (!columns.containsKey("isbn") || !columns.containsKey("name")) {
            throw new BizException("未识别到 isbn / name 表头，请下载模板后按列填写");
        }
        if (rows.size() - 1 > MAX_ROWS) {
            throw new BizException("单次最多导入 " + MAX_ROWS + " 行，请拆分文件后分批上传");
        }

        // 一次取出全部已存在 ISBN（含软删除），避免逐行查库
        Set<String> existingIsbn = new HashSet<>(bookMapper.selectAllIsbn());
        Map<String, Long> categoryIdByName = loadCategories();

        ImportResultVO result = new ImportResultVO();
        List<Book> toSave = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            int line = i + 1;
            if (isBlankRow(row)) {
                continue;
            }
            result.setTotal(result.getTotal() + 1);

            String isbn = cell(row, columns.get("isbn"));
            String name = cell(row, columns.get("name"));

            String invalid = validate(isbn, name, row, columns);
            if (invalid != null) {
                result.setFailed(result.getFailed() + 1);
                result.addIssue(line, isbn, name, "FAILED", invalid);
                continue;
            }
            if (existingIsbn.contains(isbn)) {
                result.setSkipped(result.getSkipped() + 1);
                result.addIssue(line, isbn, name, "SKIPPED", "ISBN 已存在，跳过（不会覆盖已有馆藏）");
                continue;
            }
            if (!seenInFile.add(isbn)) {
                result.setSkipped(result.getSkipped() + 1);
                result.addIssue(line, isbn, name, "SKIPPED", "文件内 ISBN 重复，仅保留首次出现");
                continue;
            }

            Book book = new Book();
            book.setIsbn(isbn);
            book.setName(name);
            String author = cell(row, columns.get("author"));
            book.setAuthor(author.isBlank() ? DEFAULT_AUTHOR : author);
            String publisher = cell(row, columns.get("publisher"));
            book.setPublisher(publisher.isBlank() ? null : publisher);
            book.setPrice(parseDecimal(cell(row, columns.get("price"))));
            String categoryName = cell(row, columns.get("category"));
            if (!categoryName.isBlank()) {
                book.setCategoryId(resolveCategoryId(categoryName, categoryIdByName));
            }
            Integer stock = parseStock(cell(row, columns.get("stock")));
            book.setStock(stock);
            book.setBorrowed(0);
            book.setStatus(1);
            book.setDeleted(0);
            String description = cell(row, columns.get("description"));
            book.setDescription(description.isBlank() ? null : description);

            toSave.add(book);
        }

        if (!toSave.isEmpty()) {
            bookService.saveBatch(toSave, 200);
        }
        result.setSuccess(toSave.size());
        log.info("图书批量导入完成：总 {} / 成功 {} / 跳过 {} / 失败 {}",
                result.getTotal(), result.getSuccess(), result.getSkipped(), result.getFailed());
        return result;
    }

    @Override
    public void writeTemplate(HttpServletResponse response) throws IOException {
        List<List<?>> sample = List.of(
                List.of("9787111213828", "深入理解Java虚拟机", "周志明", "机械工业出版社",
                        "计算机", "129.00", "3", "JVM 实现原理与性能调优经典"),
                List.of("9787506365437", "活着", "余华", "作家出版社", "文学", "28.00", "2", ""));
        CsvUtil.write(response, "book-import-template", HEADER, sample);
    }

    private void checkFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要导入的 CSV 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("文件不能超过 2MB，请拆分后分批导入");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BizException("只支持 .csv 文件（Excel 请另存为「CSV UTF-8」）");
        }
    }

    private List<List<String>> readRows(MultipartFile file) {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return CsvUtil.parse(reader);
        } catch (IOException e) {
            throw new BizException("文件读取失败，请确认文件编码为 UTF-8");
        }
    }

    /** 表头 → 列下标；未识别的列直接忽略，不影响导入 */
    private Map<String, Integer> resolveHeader(List<String> headerRow) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String raw = headerRow.get(i).replace("*", "").replace(" ", "").trim();
            if (raw.isEmpty()) {
                continue;
            }
            String field = HEADER_ALIAS.get(raw.toLowerCase(Locale.ROOT));
            if (field != null) {
                columns.putIfAbsent(field, i);
            }
        }
        return columns;
    }

    private Map<String, Long> loadCategories() {
        Map<String, Long> map = new HashMap<>();
        for (Category c : categoryService.list()) {
            map.put(c.getName().trim(), c.getId());
        }
        return map;
    }

    /** 分类按名称匹配；馆藏里没有的分类自动创建，避免导一份新书单要先去建分类 */
    private Long resolveCategoryId(String name, Map<String, Long> cache) {
        Long cached = cache.get(name);
        if (cached != null) {
            return cached;
        }
        Category category = new Category();
        category.setName(name.length() > 50 ? name.substring(0, 50) : name);
        categoryService.save(category);
        cache.put(name, category.getId());
        log.info("导入时自动创建分类：{}", category.getName());
        return category.getId();
    }

    /** 返回第一条不合法的原因；全部通过返回 null */
    private String validate(String isbn, String name, List<String> row, Map<String, Integer> columns) {
        if (isbn.isBlank()) {
            return "ISBN 为空";
        }
        if (isbn.length() > 20) {
            return "ISBN 超过 20 个字符";
        }
        if (name.isBlank()) {
            return "书名为空";
        }
        if (name.length() > 200) {
            return "书名超过 200 个字符";
        }
        String author = cell(row, columns.get("author"));
        if (author.length() > 100) {
            return "作者超过 100 个字符";
        }
        String publisher = cell(row, columns.get("publisher"));
        if (publisher.length() > 100) {
            return "出版社超过 100 个字符";
        }
        String description = cell(row, columns.get("description"));
        if (description.length() > 500) {
            return "简介超过 500 个字符";
        }
        String price = cell(row, columns.get("price"));
        if (!price.isBlank() && !isNumeric(price)) {
            return "定价「" + price + "」不是有效数字";
        }
        String stock = cell(row, columns.get("stock"));
        if (!stock.isBlank() && !stock.matches("\\d{1,6}")) {
            return "库存「" + stock + "」必须是不超过 6 位的非负整数";
        }
        return null;
    }

    private boolean isNumeric(String text) {
        try {
            new BigDecimal(text.replace("¥", "").replace(",", "").trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BigDecimal parseDecimal(String text) {
        if (text.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text.replace("¥", "").replace(",", "").trim());
    }

    private Integer parseStock(String text) {
        if (text.isBlank()) {
            return 1;
        }
        return Integer.parseInt(text.trim());
    }

    private String cell(List<String> row, Integer index) {
        if (index == null || index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value.trim();
    }

    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(v -> v == null || v.isBlank());
    }
}
