package com.concert.ticketing.notice.service;

import com.concert.ticketing.notice.dto.NoticeCreateRequest;
import com.concert.ticketing.notice.dto.NoticeUpdateRequest;
import com.concert.ticketing.notice.entity.Notice;

import java.util.List;

public interface NoticeService {

    List<Notice> listPublishedNotices();

    List<Notice> listAllNotices();

    Notice createNotice(NoticeCreateRequest req, String publisherUsername);

    Notice updateNotice(Long id, NoticeUpdateRequest req);

    void deleteNotice(Long id);
}
