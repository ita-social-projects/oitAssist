package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.Set;

public final class TaskFilePolicy implements FilePolicy {
    public static final TaskFilePolicy INSTANCE = new TaskFilePolicy();

    private TaskFilePolicy() {
    }

    @Override
    public Set<AllowedExtension> getAllowedExtensions() {
        return Set.of(
            AllowedExtension.DOCX,
            AllowedExtension.XLSX,
            AllowedExtension.PPTX,
            AllowedExtension.ACCDB);
    }

    @Override
    public int getMaxFileCount() {
        return 1;
    }
}
