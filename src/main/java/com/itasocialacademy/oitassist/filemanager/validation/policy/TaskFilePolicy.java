package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.Set;
import org.springframework.util.unit.DataSize;

public final class TaskFilePolicy implements FilePolicy {
    public static final TaskFilePolicy INSTANCE = new TaskFilePolicy();

    private TaskFilePolicy() {
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
            AllowedExtension.ACCDB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxFileCount() {
        return 1;
    }

    // TODO: confirm size limit with business
    /**
     * {@inheritDoc}
     */
    @Override
    public DataSize getMaxFileSize() {
        return DataSize.ofMegabytes(50);
    }
}
