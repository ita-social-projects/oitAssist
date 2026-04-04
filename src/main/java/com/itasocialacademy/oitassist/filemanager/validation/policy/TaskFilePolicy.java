package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.Set;

public final class TaskFilePolicy implements FilePolicy {
    public static final Set<AllowedExtension> GLOBALLY_ALLOWED = Set.of(
        AllowedExtension.DOCX,
        AllowedExtension.XLSX,
        AllowedExtension.PPTX,
        AllowedExtension.ACCDB);

    private final AllowedExtension requiredExtension;
    private final String requiredFileName;

    public TaskFilePolicy(AllowedExtension requiredExtension, String requiredFileName) {
        this.requiredExtension = requiredExtension;
        this.requiredFileName = requiredFileName;
    }

    public String getRequiredFileName() {
        return requiredFileName;
    }

    @Override
    public Set<AllowedExtension> getAllowedExtensions() {
        return Set.of(requiredExtension);
    }

    @Override
    public int getMaxFileCount() {
        return 1;
    }
}
