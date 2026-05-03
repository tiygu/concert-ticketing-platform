package com.concert.ticketing.show.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.show.dto.ShowCreateRequest;
import com.concert.ticketing.show.dto.ShowUpdateRequest;
import com.concert.ticketing.show.entity.Show;
import com.concert.ticketing.show.mapper.ShowMapper;
import com.concert.ticketing.ticket.entity.Ticket;
import com.concert.ticketing.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {

    private static final long MAX_COVER_SIZE = 2 * 1024 * 1024;
    private static final String COVER_UPLOAD_DIR = "uploads/covers";
    private static final String DEFAULT_STATUS = "ON_SALE";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final ShowMapper showMapper;
    private final TicketMapper ticketMapper;

    @Override
    public Page<Show> listShows(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<Show> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String keywordValue = keyword.trim();
            wrapper.and(query -> query.like(Show::getShowName, keywordValue)
                    .or()
                    .like(Show::getVenue, keywordValue));
        }
        wrapper.orderByDesc(Show::getShowTime);
        return showMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Show getShowById(Long id) {
        Show show = showMapper.selectById(id);
        if (show == null || Integer.valueOf(1).equals(show.getIsDeleted())) {
            throw new BizException("演出不存在");
        }
        return show;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Show createShow(ShowCreateRequest request) {
        Show show = new Show();
        show.setShowName(request.getTitle());
        show.setVenue(request.getVenue());
        show.setShowTime(request.getShowTime());
        show.setPriceRange(formatPrice(request.getTicketPrice()));
        show.setTotalSeats(request.getTotalSeats());
        show.setDescription(request.getDescription());
        show.setStatus(DEFAULT_STATUS);
        show.setIsDeleted(0);
        showMapper.insert(show);
        return show;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Show updateShow(Long id, ShowUpdateRequest request) {
        Show show = getShowById(id);
        if (request.getTitle() != null) {
            show.setShowName(request.getTitle());
        }
        if (request.getVenue() != null) {
            show.setVenue(request.getVenue());
        }
        if (request.getShowTime() != null) {
            show.setShowTime(request.getShowTime());
        }
        if (request.getTicketPrice() != null) {
            show.setPriceRange(formatPrice(request.getTicketPrice()));
        }
        if (request.getTotalSeats() != null) {
            show.setTotalSeats(request.getTotalSeats());
        }
        if (request.getDescription() != null) {
            show.setDescription(request.getDescription());
        }
        showMapper.updateById(show);
        return getShowById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShow(Long id) {
        getShowById(id);
        Show show = new Show();
        show.setId(id);
        show.setIsDeleted(1);
        showMapper.updateById(show);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadCover(Long showId, MultipartFile file) {
        Show show = getShowById(showId);
        validateCover(file);

        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        Path uploadDir = Paths.get(COVER_UPLOAD_DIR);
        Path targetPath = uploadDir.resolve(filename);

        try {
            Files.createDirectories(uploadDir);
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("Upload show cover failed: showId={}, filename={}", showId, filename, e);
            throw new BizException("封面上传失败");
        }

        String relativePath = "/uploads/covers/" + filename;
        show.setCoverImage(relativePath);
        showMapper.updateById(show);
        return relativePath;
    }

    @Override
    public int countAvailableSeats(Long showId, Integer totalSeats) {
        if (totalSeats == null) {
            return 0;
        }
        Integer soldSeats = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                        .eq(Ticket::getShowId, showId))
                .stream()
                .map(Ticket::getSold)
                .filter(sold -> sold != null)
                .reduce(0, Integer::sum);
        return Math.max(totalSeats - soldSeats, 0);
    }

    private void validateCover(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("封面文件不能为空");
        }
        if (file.getSize() > MAX_COVER_SIZE) {
            throw new BizException("封面文件大小不能超过2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BizException("封面仅支持jpg或png格式");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException("封面仅支持jpg或png格式");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BizException("封面仅支持jpg或png格式");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? null : price.stripTrailingZeros().toPlainString();
    }
}
