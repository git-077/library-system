package com.library.module.book.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 图书新增 / 编辑入参。
 * <p>不含 id、borrowed、deleted、createTime 等由服务端维护的字段；
 * 库存 stock 只在新增时生效，编辑时被忽略（由借阅事务维护）。
 * <p><b>字段长度必须与 sql/init.sql 中的列定义保持一致</b>，
 * 否则超出列长度会在 insert 时抛 DataTooLong 变成 500。
 */
@Data
public class BookDTO {

    private Long id;

    /** book.isbn VARCHAR(20) */
    @NotBlank(message = "ISBN 不能为空")
    @Size(max = 20, message = "ISBN 不能超过 20 个字符")
    private String isbn;

    /** book.name VARCHAR(200) */
    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名不能超过 200 个字符")
    private String name;

    /** book.author VARCHAR(100)，列定义为 NOT NULL */
    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者不能超过 100 个字符")
    private String author;

    /** book.publisher VARCHAR(100) */
    @Size(max = 100, message = "出版社不能超过 100 个字符")
    private String publisher;

    private Long categoryId;

    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal price;

    @Min(value = 0, message = "库存数量不合法")
    private Integer stock;

    /** book.description VARCHAR(500) */
    @Size(max = 500, message = "简介不能超过 500 个字符")
    private String description;

    /** 0下架 1上架，新增时为空默认上架 */
    private Integer status;
}
