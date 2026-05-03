package com.concert.ticketing.ticket.service;

import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.ticket.entity.Ticket;
import com.concert.ticketing.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Ticket lockSeat(Long ticketId) {
        // Step 1: 行级锁 —— SELECT ... FOR UPDATE
        Ticket ticket = ticketMapper.selectByIdForUpdate(ticketId)
                .orElseThrow(() -> new BizException(404, "选票不存在"));

        // Step 2: 校验库存
        if (ticket.getStock() <= 0) {
            throw new BizException(409, "座位库存不足，已被其他用户锁定");
        }

        // Step 3: 原子扣减库存（防超售）
        int affected = ticketMapper.deductStock(ticketId);
        if (affected == 0) {
            throw new BizException(409, "库存扣减失败，座位可能已被其他用户锁定");
        }

        log.info("Seat locked: ticketId={}, seatNumber={}, remainingStock={}",
                ticketId, ticket.getSeatNumber(), ticket.getStock() - 1);

        ticket.setStock(ticket.getStock() - 1);
        ticket.setSold(ticket.getSold() + 1);
        return ticket;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeat(Long ticketId) {
        int affected = ticketMapper.releaseStock(ticketId);
        if (affected == 0) {
            log.warn("Release seat failed: ticketId={}, no stock to release (sold=0)", ticketId);
            return;
        }
        log.info("Seat released: ticketId={}", ticketId);
    }

    @Override
    public List<Ticket> getByShowId(Long showId) {
        return ticketMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getShowId, showId)
        );
    }

    @Override
    public Ticket getById(Long id) {
        return ticketMapper.selectById(id);
    }
}
