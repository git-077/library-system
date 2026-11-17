package com.library.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约记录（状态流转而非删除，便于统计「这本书有多热门」）。
 */
@Data
@TableName("reservation")
public class Reservation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    /** 见 ReservationStatus */
    private Integer status;

    /**
     * 有效标记：进行中为 1，终态为 NULL。
     * <p>与唯一索引 {@code (user_id, book_id, active_flag)} 配合，
     * 实现「一人一书只能有一条进行中的预约」，同时不影响历史记录累积。
     */
    private Integer activeFlag;

    /** 到书通知时间 */
    private LocalDateTime readyTime;

    /** 保留截止时间：到馆后 N 天未取则失效 */
    private LocalDateTime expireTime;

    /** 结束时间（取消 / 过期 / 借出） */
    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /* ---------------- 非表字段：列表展示用 ---------------- */

    /** 书名（关联查询填充） */
    @TableField(exist = false)
    private String bookName;

    /** 作者（关联查询填充） */
    @TableField(exist = false)
    private String bookAuthor;

    /** 封面（关联查询填充） */
    @TableField(exist = false)
    private String coverUrl;

    /** 预约人用户名（管理端列表填充） */
    @TableField(exist = false)
    private String username;

    /** 排队位置：仅 status=0 有意义，1 表示队首 */
    @TableField(exist = false)
    private Integer position;
}
