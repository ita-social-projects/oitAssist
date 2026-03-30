package com.itasocialacademy.oitassist.filemanager.mapper;

import com.itasocialacademy.oitassist.core.rest.mapper.CreateMapper;
import com.itasocialacademy.oitassist.core.rest.mapper.DtoMapper;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMapper extends DtoMapper<FileAsset, FileResponseDto>,
    CreateMapper<FileAsset, FileUploadRequestDto> {
}
