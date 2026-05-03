package com.concert.ticketing.user.dto;

import com.concert.ticketing.user.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileResponse {

    private Long id;
    private String username;
    private String role;
    private String phone;
    private String email;
    private Integer vipLevel;
    private Integer points;
    private String status;
    private LocalDateTime createdAt;

    public static UserProfileResponse fromUser(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setVipLevel(user.getVipLevel());
        response.setPoints(user.getPoints());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
