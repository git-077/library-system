package com.library.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.module.reservation.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 我前面还有几个人在排队。
     *
     * <p>队列顺序直接用自增主键（先到先得，插入顺序即排队顺序），
     * 因此「排第几」= 同书、同状态、id 比我小的行数 + 1。
     * 不用 create_time 排序是因为同一秒可能有多人预约，时间戳分不出先后。
     */
    @Select("SELECT COUNT(*) FROM reservation " +
            "WHERE book_id = #{bookId} AND status = 0 AND id < #{id}")
    int countAhead(@Param("bookId") Long bookId, @Param("id") Long id);
}
