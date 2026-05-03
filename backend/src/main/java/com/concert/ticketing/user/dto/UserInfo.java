package com.concert.ticketing.user.dto;

import com.concert.ticketing.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {

    private Long id;

    private String username;

    private String role;

    private Integer vipLevel;

    public static UserInfo fromUser(User user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getRole(), user.getVipLevel());
    }
}
