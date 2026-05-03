package com.concert.ticketing.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.security.JwtUtil;
import com.concert.ticketing.user.dto.LoginRequest;
import com.concert.ticketing.user.dto.LoginResponse;
import com.concert.ticketing.user.dto.RegisterRequest;
import com.concert.ticketing.user.dto.UserInfo;
import com.concert.ticketing.user.entity.User;
import com.concert.ticketing.user.mapper.UserMapper;
import com.concert.ticketing.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "USER";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<LoginResponse> register(RegisterRequest req) {
        if (findByUsername(req.getUsername()) != null) {
            throw new BizException(400, "用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(DEFAULT_ROLE);
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setVipLevel(0);
        user.setPoints(0);
        user.setStatus(ACTIVE_STATUS);
        user.setIsDeleted(0);
        userMapper.insert(user);

        log.info("User registered: userId={}, username={}", user.getId(), user.getUsername());
        return Result.ok(buildLoginResponse(user));
    }

    @Override
    public Result<LoginResponse> login(LoginRequest req) {
        User user = findByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        validateActiveUser(user);

        log.info("User login succeeded: userId={}, username={}", user.getId(), user.getUsername());
        return Result.ok(buildLoginResponse(user));
    }

    @Override
    public Result<LoginResponse> refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(400, "刷新令牌不能为空");
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BizException(401, "刷新令牌无效或已过期");
        }
        if (StringUtils.hasText(jwtUtil.getRoleFromToken(refreshToken))) {
            throw new BizException(401, "刷新令牌无效或已过期");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        if (!StringUtils.hasText(username)) {
            throw new BizException(401, "刷新令牌无效或已过期");
        }

        User user = findByUsername(username);
        if (user == null) {
            throw new BizException(401, "刷新令牌无效或已过期");
        }
        validateActiveUser(user);

        return Result.ok(buildLoginResponse(user));
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        return new LoginResponse(accessToken, refreshToken, UserInfo.fromUser(user));
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    private void validateActiveUser(User user) {
        if (!ACTIVE_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new BizException(403, "账号状态异常");
        }
    }
}
