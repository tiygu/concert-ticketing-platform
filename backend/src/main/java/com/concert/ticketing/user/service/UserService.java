package com.concert.ticketing.user.service;

import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.user.dto.LoginRequest;
import com.concert.ticketing.user.dto.LoginResponse;
import com.concert.ticketing.user.dto.RegisterRequest;

public interface UserService {

    Result<LoginResponse> register(RegisterRequest req);

    Result<LoginResponse> login(LoginRequest req);

    Result<LoginResponse> refresh(String refreshToken);
}
