package com.itasocialacademy.oitassist.filemanager.service.interfaces;

import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    List<FileResponseDto> upload(List<MultipartFile> files, FileUploadRequestDto requestDto, Long userId);
}
