package com.itasocialacademy.oitassist.filemanager.service.interfaces;

import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * Validates and uploads a batch of files, linking them to the specified entity.
     *
     * @param files      the files to upload
     * @param requestDto upload context metadata (entity type and optional entity
     *                   ID)
     * @param userId     the ID of the user performing the upload
     * @return a list of {@link FileResponseDto} representing the persisted file
     *         records
     * @throws com.itasocialacademy.oitassist.core.exceptions.ValidationException if
     *                                                                            any
     *                                                                            file
     *                                                                            fails
     *                                                                            the
     *                                                                            policy
     *                                                                            validation
     */
    List<FileResponseDto> upload(List<MultipartFile> files, FileUploadRequestDto requestDto, Long userId);

    /**
     * Method to mark a file SOFT_DELETED, but keep a physical file intact.
     *
     * @param fileId id of the file record in the db.
     */
    void deleteSoft(Long fileId);

    /**
     * Method to mark a file HARD_DELETED, and call StorageProvider to physically
     * delete the file.
     *
     * @param fileId id of the file record in the db.
     */
    void deleteHard(Long fileId);
}
