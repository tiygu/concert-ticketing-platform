package com.concert.ticketing.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.ticketing.common.dto.PageResult;
import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.user.dto.UserManagementResponse;
import com.concert.ticketing.user.dto.UserProfileResponse;
import com.concert.ticketing.user.dto.UserProfileUpdateRequest;
import com.concert.ticketing.user.dto.UserStatusUpdateRequest;
import com.concert.ticketing.user.entity.User;
import com.concert.ticketing.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UserController {

    private static final String ACTIVE = "ACTIVE";
    private static final String DISABLED = "DISABLED";

    private final UserMapper userMapper;

    @GetMapping("/api/admin/users")
    public Result<PageResult<UserManagementResponse>> listUsers(@RequestParam(name = "page", defaultValue = "1") int page,
                                                                @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                                                                @RequestParam(name = "keyword", required = false) String keyword) {
        Page<User> userPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> countWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            countWrapper.and(query -> query.like(User::getUsername, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }

        long total = userMapper.selectCount(countWrapper);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(User::getUsername, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getEmail, keyword));
        }
        wrapper.orderByDesc(User::getId);

        Page<User> resultPage = userMapper.selectPage(userPage, wrapper);
        List<UserManagementResponse> records = resultPage.getRecords().stream()
                .map(UserManagementResponse::from)
                .collect(Collectors.toList());
        return Result.ok(new PageResult<>(records, total, page, pageSize));
    }

    @PutMapping("/api/admin/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable("id") Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        String status = request.getStatus();
        if (!ACTIVE.equals(status) && !DISABLED.equals(status)) {
            throw new BizException(400, "无效的状态值");
        }

        User update = new User();
        update.setId(id);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(update);
        return Result.ok();
    }

    @GetMapping("/api/user/profile")
    public Result<UserProfileResponse> getProfile() {
        User user = findCurrentUser();
        return Result.ok(UserProfileResponse.fromUser(user));
    }

    @PutMapping("/api/user/profile")
    public Result<UserProfileResponse> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        User user = findCurrentUser();

        User update = new User();
        update.setId(user.getId());
        if (request.getPhone() != null) {
            update.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            update.setEmail(request.getEmail());
        }
        update.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(update);

        return Result.ok(UserProfileResponse.fromUser(findCurrentUser()));
    }

    private User findCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }
}
