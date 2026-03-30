package com.itasocialacademy.oitassist.news.mapper.request;

import com.itasocialacademy.oitassist.core.rest.mapper.GeneralMapper;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.model.News;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NewsMapper extends GeneralMapper<News, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto> {
    @Override
    ResponseNewsDto toDto(News news);

    @Override
    News toEntity(CreateNewsDTO newsDTO);

    @Override
    void merge(UpdateNewsDto newsDTO, @MappingTarget News news);
}
