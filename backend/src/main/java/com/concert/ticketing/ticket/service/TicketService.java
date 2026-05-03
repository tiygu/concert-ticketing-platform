package com.concert.ticketing.ticket.service;

import com.concert.ticketing.ticket.entity.Ticket;

import java.util.List;

public interface TicketService {

    /**
     * 锁定座位（行级锁 + 库存扣减），返回锁定的选票
     */
    Ticket lockSeat(Long ticketId);

    /**
     * 释放座位库存
     */
    void releaseSeat(Long ticketId);

    /**
     * 查询演出所有选票
     */
    List<Ticket> getByShowId(Long showId);

    /**
     * 根据ID查询
     */
    Ticket getById(Long id);
}
