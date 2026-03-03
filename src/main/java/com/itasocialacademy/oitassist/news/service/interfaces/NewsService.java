package com.itasocialacademy.oitassist.news.service.interfaces;

import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;

public interface NewsService extends BaseService<Long, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto> {
}
