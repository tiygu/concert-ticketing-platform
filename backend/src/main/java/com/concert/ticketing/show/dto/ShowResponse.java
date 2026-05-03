package com.concert.ticketing.show.dto;

import com.concert.ticketing.show.entity.Show;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowResponse {

    private Long id;
    private String title;
    private String showName;
    private String venue;
    private LocalDateTime showTime;
    private BigDecimal ticketPrice;
    private String priceRange;
    private Integer totalSeats;
    private Integer availableSeats;
    private String description;
    private String coverImage;
    private String seatZones;
    private String status;
    private String statusText;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ShowResponse from(Show show, Integer availableSeats) {
        ShowResponse response = new ShowResponse();
        response.setId(show.getId());
        response.setTitle(show.getShowName());
        response.setShowName(show.getShowName());
        response.setVenue(show.getVenue());
        response.setShowTime(show.getShowTime());
        response.setTicketPrice(parseTicketPrice(show.getPriceRange()));
        response.setPriceRange(show.getPriceRange());
        response.setTotalSeats(show.getTotalSeats());
        response.setAvailableSeats(availableSeats);
        response.setDescription(show.getDescription());
        response.setCoverImage(show.getCoverImage());
        response.setStatus(show.getStatus());
        response.setStatusText(resolveStatusText(show));
        response.setIsDeleted(show.getIsDeleted());
        response.setCreatedAt(show.getCreatedAt());
        response.setUpdatedAt(show.getUpdatedAt());
        return response;
    }

    private static BigDecimal parseTicketPrice(String priceRange) {
        if (priceRange == null || priceRange.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(priceRange);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String resolveStatusText(Show show) {
        if (Integer.valueOf(1).equals(show.getIsDeleted())) {
            return "已下架";
        }
        String status = show.getStatus();
        if ("UPCOMING".equalsIgnoreCase(status)) {
            return "即将开售";
        }
        if ("ON_SALE".equalsIgnoreCase(status) || "SELLING".equalsIgnoreCase(status)) {
            return "售票中";
        }
        if ("FINISHED".equalsIgnoreCase(status) || "ENDED".equalsIgnoreCase(status)) {
            return "已结束";
        }
        if (show.getShowTime() != null && show.getShowTime().isBefore(LocalDateTime.now())) {
            return "已结束";
        }
        return "售票中";
    }
}
