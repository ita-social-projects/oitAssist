package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.exceptions.FileAssetNotFoundException;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.exceptions.UnsupportedStorageException;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final List<StorageProvider> providers;
    private final FileRepository repository;

    @Override
    @Transactional
    public Long upload() {
        return 0L;
    }

    /**
     * Method to perform a status change of the file. It marks the file as
     * SOFT_DELETED, which can be used in further storage cleanup operations either
     * manual, or scheduled. The physical record of the file after method execution
     * remains intact.
     *
     * @param fileId id of the file.
     */
    @Transactional
    public void deleteSoft(Long fileId) {
        FileAsset file = repository.findById(fileId)
            .orElseThrow(() -> new FileAssetNotFoundException("File not found in the database: " + fileId));

        // Implement role check with security service.
        // Throw exception if current user has insufficient authority

        file.setStatus(FileStatus.SOFT_DELETED);
        file.setDeletedAt(OffsetDateTime.now());
        repository.save(file);
    }

    /**
     * Method to handle physical deletion of a file. Used for permanent deletion,
     * cleanup scheduling or orphaned files' cleanup.
     *
     * @param fileId id of the file.
     */
    @Transactional
    public void deleteHard(Long fileId) {
        FileAsset file = repository.findById(fileId)
            .orElseThrow(() -> new FileAssetNotFoundException("File not found in the database: " + fileId));

        // Implement role check with security service.
        // Throw exception if current user has insufficient authority

        StorageProvider provider = providers.stream()
            .filter(p -> p.supports(file.getStorageProvider()))
            .findFirst()
            .orElseThrow(() -> new UnsupportedStorageException("Unsupported storage source: "
                + file.getStorageProvider()));

        String originalStorageKey = file.getStorageKey();

        try {
            // check SharePoint compatibility
            provider.deletePhysical(originalStorageKey);

            file.setStatus(FileStatus.HARD_DELETED);
            file.setDeletedAt(OffsetDateTime.now());
            file.setStorageKey("");
        } catch (Exception e) {
            log.error("Physical deletion failed for file {}, but DB record updated", fileId, e);
            file.setStatus(FileStatus.FAILED);
        }

        repository.save(file);
    }
}
