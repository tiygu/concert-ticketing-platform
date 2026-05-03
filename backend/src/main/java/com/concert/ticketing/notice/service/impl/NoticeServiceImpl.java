package com.concert.ticketing.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.notice.dto.NoticeCreateRequest;
import com.concert.ticketing.notice.dto.NoticeUpdateRequest;
import com.concert.ticketing.notice.entity.Notice;
import com.concert.ticketing.notice.mapper.NoticeMapper;
import com.concert.ticketing.user.entity.User;
import com.concert.ticketing.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;

    @Override
    public List<Notice> listPublishedNotices() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Notice::getPublishTime));
    }

    @Override
    public List<Notice> listAllNotices() {
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notice createNotice(NoticeCreateRequest req, String publisherUsername) {
        User publisher = userMapper.selectOne(new QueryWrapper<User>().eq("username", publisherUsername));
        if (publisher == null) {
            throw new BizException(404, "用户不存在");
        }

        Notice notice = new Notice();
        notice.setTitle(req.getTitle());
        notice.setContent(req.getContent());
        notice.setPublishTime(req.getPublishTime());
        notice.setStatus(req.getPublishTime() != null ? STATUS_PUBLISHED : STATUS_DRAFT);
        notice.setPublisherId(publisher.getId());
        noticeMapper.insert(notice);
        return notice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notice updateNotice(Long id, NoticeUpdateRequest req) {
        Notice notice = getNoticeById(id);
        notice.setTitle(req.getTitle());
        notice.setContent(req.getContent());
        notice.setStatus(req.getStatus());
        notice.setPublishTime(req.getPublishTime());
        noticeMapper.updateById(notice);
        return getNoticeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        Notice notice = getNoticeById(id);
        notice.setStatus(STATUS_ARCHIVED);
        noticeMapper.updateById(notice);
    }

    private Notice getNoticeById(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException(404, "公告不存在");
        }
        return notice;
    }
}
