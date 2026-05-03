package com.concert.ticketing.notice.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class NoticeUpdateRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200个字符")
    private String title;

    @Size(max = 5000, message = "公告内容不能超过5000个字符")
    private String content;

    private String status;

    private LocalDateTime publishTime;
}
