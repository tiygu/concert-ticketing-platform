package com.concert.ticketing.ticket.controller;

import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.ticket.dto.SeatResponse;
import com.concert.ticketing.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class SeatController {

    private final TicketService ticketService;

    @GetMapping("/api/shows/{showId}/seats")
    public Result<List<SeatResponse>> listSeats(@PathVariable Long showId) {
        List<SeatResponse> seats = ticketService.getByShowId(showId).stream()
                .map(SeatResponse::from)
                .collect(Collectors.toList());
        return Result.ok(seats);
    }
}
