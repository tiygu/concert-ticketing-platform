package com.concert.ticketing.notice.dto;

import com.concert.ticketing.notice.entity.Notice;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeResponse {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime publishTime;
    private String status;
    private String statusText;
    private Long publisherId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoticeResponse from(Notice notice) {
        NoticeResponse response = new NoticeResponse();
        response.setId(notice.getId());
        response.setTitle(notice.getTitle());
        response.setContent(notice.getContent());
        response.setPublishTime(notice.getPublishTime());
        response.setStatus(notice.getStatus());
        response.setStatusText(resolveStatusText(notice.getStatus()));
        response.setPublisherId(notice.getPublisherId());
        response.setCreatedAt(notice.getCreatedAt());
        response.setUpdatedAt(notice.getUpdatedAt());
        return response;
    }

    private static String resolveStatusText(String status) {
        if (status == null) {
            return null;
        }
        if ("DRAFT".equalsIgnoreCase(status)) {
            return "草稿";
        }
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            return "已发布";
        }
        if ("ARCHIVED".equalsIgnoreCase(status)) {
            return "已归档";
        }
        return status;
    }
}
