package com.concert.ticketing.notice.controller;

import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.notice.dto.NoticeCreateRequest;
import com.concert.ticketing.notice.dto.NoticeResponse;
import com.concert.ticketing.notice.dto.NoticeUpdateRequest;
import com.concert.ticketing.notice.entity.Notice;
import com.concert.ticketing.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/api/notices")
    public Result<List<NoticeResponse>> listPublishedNotices() {
        List<NoticeResponse> notices = noticeService.listPublishedNotices().stream()
                .map(NoticeResponse::from)
                .collect(Collectors.toList());
        return Result.ok(notices);
    }

    @GetMapping("/api/admin/notices")
    public Result<List<NoticeResponse>> listAllNotices() {
        List<NoticeResponse> notices = noticeService.listAllNotices().stream()
                .map(NoticeResponse::from)
                .collect(Collectors.toList());
        return Result.ok(notices);
    }

    @PostMapping("/api/admin/notices")
    public Result<NoticeResponse> createNotice(@Valid @RequestBody NoticeCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Notice notice = noticeService.createNotice(request, authentication.getName());
        return Result.ok(NoticeResponse.from(notice));
    }

    @PutMapping("/api/admin/notices/{id}")
    public Result<NoticeResponse> updateNotice(@PathVariable Long id,
                                               @Valid @RequestBody NoticeUpdateRequest request) {
        Notice notice = noticeService.updateNotice(id, request);
        return Result.ok(NoticeResponse.from(notice));
    }

    @DeleteMapping("/api/admin/notices/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return Result.ok();
    }
}
