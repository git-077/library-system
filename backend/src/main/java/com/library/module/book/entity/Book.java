package com.library.module.book.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书实体
 * stock（在馆库存）+ borrowed（已借出）= 总藏书量，由借阅事务原子维护
 */
@Data
@TableName("book")
public class Book implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String isbn;

    private String name;

    private String author;

    private String publisher;

    private Long categoryId;

    /** 分类名称（关联查询填充，非表字段） */
    @TableField(exist = false)
    private String categoryName;

    private BigDecimal price;

    /** 在馆库存 */
    private Integer stock;

    /** 已借出数量 */
    private Integer borrowed;

    /** 0下架 1上架 */
    private Integer status;

    private String description;

    /** 软删除 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
