package com.library.module.reservation.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书详情页的预约态：一次查询把「这本书排队多少人、我处在什么状态」全部带回去，
 * 避免前端为一个按钮连发三个请求。
 */
@Data
public class BookReservationVO {

    /** 排队中人数（不含待取书） */
    private long waitingCount;

    /** 待取书人数（这些人各自锁着一份副本） */
    private long readyCount;

    /** 我的预约状态：null 表示未预约 */
    private Integer myStatus;

    /** 我的预约ID（取消时用） */
    private Long myReservationId;

    /** 我的排队位置：仅排队中有值 */
    private Integer myPosition;

    /** 我的保留截止时间：仅待取书有值 */
    private LocalDateTime myExpireTime;

    /** 到馆保留天数（前端文案用：「到书后可保留 N 天」） */
    private Integer holdDays;
}
