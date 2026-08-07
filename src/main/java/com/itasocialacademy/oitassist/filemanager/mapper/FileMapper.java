package com.itasocialacademy.oitassist.filemanager.mapper;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileResponseDto toDto(FileAsset entity);

    @Mapping(target = "fileRole", expression = "java(entity.getFileRole().name())")
    FileDetailsDTO toDetails(FileAsset entity, String url);
}
