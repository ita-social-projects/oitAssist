package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileReadException;
import com.itasocialacademy.oitassist.filemanager.mapper.FileMapper;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final FileRepository fileRepository;
    private final FileMapper fileMapper;
    private final StorageProvider storageProvider;

    @Override
    @Transactional
    public List<FileResponseDto> upload(List<MultipartFile> files, FileUploadRequestDto requestDto, Long userId) {
        return files.stream()
            .map(file -> uploadSingle(file, requestDto, userId))
            .toList();
    }

    private FileResponseDto uploadSingle(MultipartFile file, FileUploadRequestDto requestDto, Long userId) {
        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateStoredFilename(originalFilename);
        String relativePath = buildRelativePath(requestDto);

        String storageKey;
        try {
            storageKey = storageProvider.upload(file.getInputStream(), storedFilename, relativePath);
        } catch (IOException e) {
            log.error("Failed to read input stream for file: {}", originalFilename, e);
            throw new FileReadException(e);
        }

        FileAsset fileAsset = buildFileAsset(file, requestDto, originalFilename, storedFilename, storageKey, userId);
        FileAsset saved = fileRepository.save(fileAsset);
        return fileMapper.toDto(saved);
    }

    private String generateStoredFilename(String originalFilename) {
        return UUID.randomUUID() + extractExtension(originalFilename);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    private String buildRelativePath(FileUploadRequestDto requestDto) {
        return requestDto.getRelatedEntityType().name().toLowerCase();
    }

    private FileAsset buildFileAsset(
        MultipartFile file,
        FileUploadRequestDto requestDto,
        String originalFilename,
        String storedFilename,
        String storageKey,
        Long userId) {
        return FileAsset.builder()
            .userId(userId)
            .relatedEntityType(requestDto.getRelatedEntityType())
            .relatedEntityId(requestDto.getRelatedEntityId())
            .status(resolveStatus(requestDto))
            .storageProvider(StorageProviderType.LOCAL)
            .originalFilename(originalFilename)
            .storedFilename(storedFilename)
            .storageKey(storageKey)
            .mimeType(file.getContentType())
            .size(file.getSize())
            .build();
    }

    private FileStatus resolveStatus(FileUploadRequestDto requestDto) {
        return requestDto.getRelatedEntityId() == null
            ? FileStatus.TEMPORARY
            : FileStatus.ATTACHED;
    }
}
