package com.itasocialacademy.oitassist.news.mapper.request;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsAdminListItemDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import com.itasocialacademy.oitassist.news.dao.model.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface NewsMapper {
    ResponseNewsDto toDto(News news);

    @Mapping(target = "contentPreview", source = "content", qualifiedByName = "buildPreview")
    ResponseNewsListItemDto toListItemDto(News news);

    @Mapping(target = "contentPreview", source = "content", qualifiedByName = "buildPreview")
    ResponseNewsAdminListItemDto toAdminListItemDto(News news);

    News toEntity(CreateNewsDTO newsDTO);

    void merge(UpdateNewsDto newsDTO, @MappingTarget News news);

    @Named("buildPreview")
    static String buildPreview(String content) {
        if (content == null) {
            return null;
        }
        int previewLength = 300;
        return content.length() > previewLength ? content.substring(0, previewLength) + "..." : content;
    }
}
