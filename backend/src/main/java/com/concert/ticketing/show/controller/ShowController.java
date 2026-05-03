package com.concert.ticketing.show.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.ticketing.common.dto.PageResult;
import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.show.dto.ShowCreateRequest;
import com.concert.ticketing.show.dto.ShowResponse;
import com.concert.ticketing.show.dto.ShowUpdateRequest;
import com.concert.ticketing.show.entity.Show;
import com.concert.ticketing.show.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    // TODO: Add auth check when #3 is complete
    @GetMapping("/api/shows")
    public Result<PageResult<ShowResponse>> listShows(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      @RequestParam(required = false) String keyword) {
        Page<Show> showPage = showService.listShows(page, pageSize, keyword);
        List<ShowResponse> records = showPage.getRecords().stream()
                .map(show -> ShowResponse.from(show, showService.countAvailableSeats(show.getId(), show.getTotalSeats())))
                .collect(Collectors.toList());
        return Result.ok(new PageResult<>(records, showPage.getTotal(), page, pageSize));
    }

    // TODO: Add auth check when #3 is complete
    @GetMapping("/api/shows/{id}")
    public Result<ShowResponse> getShow(@PathVariable Long id) {
        Show show = showService.getShowById(id);
        return Result.ok(ShowResponse.from(show, showService.countAvailableSeats(show.getId(), show.getTotalSeats())));
    }

    // TODO: Require ADMIN role after #3 auth implementation
    @PostMapping("/api/admin/shows")
    public Result<ShowResponse> createShow(@Valid @ModelAttribute ShowCreateRequest request,
                                           @RequestPart(value = "cover", required = false) MultipartFile cover) {
        Show show = showService.createShow(request);
        if (cover != null && !cover.isEmpty()) {
            showService.uploadCover(show.getId(), cover);
            show = showService.getShowById(show.getId());
        }
        return Result.ok(ShowResponse.from(show, showService.countAvailableSeats(show.getId(), show.getTotalSeats())));
    }

    // TODO: Require ADMIN role after #3 auth implementation
    @PutMapping("/api/admin/shows/{id}")
    public Result<ShowResponse> updateShow(@PathVariable Long id,
                                           @Valid @ModelAttribute ShowUpdateRequest request,
                                           @RequestPart(value = "cover", required = false) MultipartFile cover) {
        Show show = showService.updateShow(id, request);
        if (cover != null && !cover.isEmpty()) {
            showService.uploadCover(show.getId(), cover);
            show = showService.getShowById(show.getId());
        }
        return Result.ok(ShowResponse.from(show, showService.countAvailableSeats(show.getId(), show.getTotalSeats())));
    }

    // TODO: Require ADMIN role after #3 auth implementation
    @DeleteMapping("/api/admin/shows/{id}")
    public Result<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return Result.ok();
    }
}
