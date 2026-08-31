package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
public class TaskProblemFilePolicy implements FilePolicy {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(RelatedEntityType entityType, FileRole role) {
        return entityType == RelatedEntityType.TASK && role == FileRole.PROBLEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AllowedExtension> getAllowedExtensions() {
        return Set.of(
            AllowedExtension.DOCX,
            AllowedExtension.XLSX,
            AllowedExtension.PPTX,
            AllowedExtension.ACCDB,
            AllowedExtension.JPG,
            AllowedExtension.JPEG,
            AllowedExtension.PNG,
            AllowedExtension.GIF,
            AllowedExtension.WEBP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxFileCount() {
        return 10;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSize getMaxFileSize() {
        return DataSize.ofMegabytes(50);
    }
}
