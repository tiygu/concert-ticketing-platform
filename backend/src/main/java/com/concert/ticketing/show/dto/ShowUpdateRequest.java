package com.concert.ticketing.show.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShowUpdateRequest {

    @Size(max = 100, message = "演出标题不能超过100个字符")
    private String title;

    @Size(max = 100, message = "演出场馆不能超过100个字符")
    private String venue;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime showTime;

    @DecimalMin(value = "0.00", message = "票价不能小于0")
    private BigDecimal ticketPrice;

    @Min(value = 1, message = "座位总数必须大于0")
    private Integer totalSeats;

    @Size(max = 2000, message = "演出描述不能超过2000个字符")
    private String description;

    @Size(max = 4000, message = "座位区域数据不能超过4000个字符")
    private String seatZones;
}
