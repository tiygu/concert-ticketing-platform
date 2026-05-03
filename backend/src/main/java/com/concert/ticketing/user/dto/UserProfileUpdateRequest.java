package com.concert.ticketing.user.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class UserProfileUpdateRequest {

    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;

    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;
}
