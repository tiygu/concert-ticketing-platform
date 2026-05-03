package com.concert.ticketing.ticket.dto;

import com.concert.ticketing.ticket.entity.Ticket;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeatResponse {

    private Long id;
    private Long showId;
    private String seatNumber;
    private String ticketType;
    private String priceType;
    private BigDecimal price;
    private Integer pointsPrice;
    private Integer stock;
    private Integer sold;
    private boolean available;

    public static SeatResponse from(Ticket ticket) {
        SeatResponse response = new SeatResponse();
        response.setId(ticket.getId());
        response.setShowId(ticket.getShowId());
        response.setSeatNumber(ticket.getSeatNumber());
        response.setTicketType(ticket.getTicketType());
        response.setPriceType(ticket.getPriceType());
        response.setPrice(ticket.getPrice());
        response.setPointsPrice(ticket.getPointsPrice());
        response.setStock(ticket.getStock());
        response.setSold(ticket.getSold());
        response.setAvailable(ticket.getStock() != null && ticket.getStock() > 0);
        return response;
    }
}
