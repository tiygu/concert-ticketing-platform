package com.concert.ticketing.show.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.ticketing.show.dto.ShowCreateRequest;
import com.concert.ticketing.show.dto.ShowUpdateRequest;
import com.concert.ticketing.show.entity.Show;
import org.springframework.web.multipart.MultipartFile;

public interface ShowService {

    Page<Show> listShows(int page, int pageSize, String keyword);

    Show getShowById(Long id);

    Show createShow(ShowCreateRequest request);

    Show updateShow(Long id, ShowUpdateRequest request);

    void deleteShow(Long id);

    String uploadCover(Long showId, MultipartFile file);

    int countAvailableSeats(Long showId, Integer totalSeats);
}
