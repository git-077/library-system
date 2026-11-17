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

    /** 在馆库存（只算「任何人都能借」的副本，不含被预约锁定的） */
    private Integer stock;

    /** 已借出数量 */
    private Integer borrowed;

    /**
     * 已被到书预约锁定的副本数。
     * <p>恒等式：{@code stock + held + borrowed = 馆藏总量}。
     * 副本从「已借出」归还时，若有人在排队就直接进 held（锁定给队首），
     * 不进 stock，因此别人借不走；预约者借出或预约失效时再从中扣减。
     */
    private Integer held;

    /** 0下架 1上架 */
    private Integer status;

    private String description;

    /** 封面图地址：站内相对路径（/covers/xxx.jpg）或完整 URL；为空时前端降级为配色书封 */
    private String coverUrl;

    /** 软删除 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
