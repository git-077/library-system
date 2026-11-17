package com.library.module.book.service;

import com.library.module.book.dto.ImportResultVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 图书批量导入（CSV）。
 */
public interface BookImportService {

    /**
     * 解析并导入 CSV。校验通过的行才会写入，坏行逐条记录原因，不中断其余行。
     */
    ImportResultVO importCsv(MultipartFile file);

    /** 输出导入模板（带表头与两行示例，UTF-8 BOM 保证 Excel 打开不乱码） */
    void writeTemplate(HttpServletResponse response) throws IOException;
}
