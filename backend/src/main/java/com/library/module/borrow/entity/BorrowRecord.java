package com.library.module.borrow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 借阅记录实体（归还后记录保留，状态流转而非删除）
 */
@Data
@TableName("borrow_record")
public class BorrowRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    /** 冗余展示字段：借阅人用户名（关联查询填充，非表字段） */
    @TableField(exist = false)
    private String username;

    /** 冗余展示字段：书名（关联查询填充，非表字段） */
    @TableField(exist = false)
    private String bookName;

    private LocalDateTime borrowDate;

    /** 应还时间：逾期判断依据 */
    private LocalDateTime dueDate;

    private LocalDateTime returnDate;

    /** 0借出 1已还 */
    private Integer status;

    /** 已续借次数 */
    private Integer renewCount;

    /** 逾期标记：0 正常 / 1 逾期未还，由每日定时任务刷新，仅作展示与筛选 */
    private Integer overdue;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
