package com.itasocialacademy.oitassist.news.service.interfaces;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsAdminListItemDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsService {
    ResponseNewsDto save(CreateNewsDTO dto);

    ResponseNewsDto update(UpdateNewsDto dto);

    void delete(Long id);

    ResponseNewsDto getById(Long id);

    Page<ResponseNewsListItemDto> getPublishedNews(Pageable pageable, String search, LocalDate date);

    Page<ResponseNewsAdminListItemDto> getAllNewsForAdmin(Pageable pageable, String search, List<NewsStatus> statuses,
        LocalDate dateFrom, LocalDate dateTo);
}
