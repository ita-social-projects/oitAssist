package com.itasocialacademy.oitassist.news.service.interfaces;

import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsService extends BaseService<Long, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto> {
    Page<ResponseNewsListItemDto> getPublishedNews(Pageable pageable);
}
