package com.itasocialacademy.oitassist.filemanager.mapper;

import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileResponseDto toDto(FileAsset entity);
}
