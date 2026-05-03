package com.concert.ticketing.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concert.ticketing.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 行级锁查询：SELECT ... FOR UPDATE，用于锁定单张选票记录
     */
    @Select("SELECT * FROM tickets WHERE id = #{ticketId} FOR UPDATE")
    Optional<Ticket> selectByIdForUpdate(@Param("ticketId") Long ticketId);

    /**
     * 原子扣减库存（stock > 0 时才执行，防超售）
     */
    @Update("UPDATE tickets SET stock = stock - 1, sold = sold + 1 " +
            "WHERE id = #{ticketId} AND stock > 0")
    int deductStock(@Param("ticketId") Long ticketId);

    /**
     * 释放库存（取消订单/超时释放时使用）
     */
    @Update("UPDATE tickets SET stock = stock + 1, sold = sold - 1 " +
            "WHERE id = #{ticketId} AND sold > 0")
    int releaseStock(@Param("ticketId") Long ticketId);
}
