package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.Set;

public final class NewsFilePolicy implements FilePolicy {
    public static final NewsFilePolicy INSTANCE = new NewsFilePolicy();

    private NewsFilePolicy() {
    }

    @Override
    public Set<AllowedExtension> getAllowedExtensions() {
        return Set.of(
            AllowedExtension.JPG,
            AllowedExtension.JPEG,
            AllowedExtension.PNG,
            AllowedExtension.GIF,
            AllowedExtension.WEBP);
    }

    @Override
    public int getMaxFileCount() {
        return 10;
    }
}
