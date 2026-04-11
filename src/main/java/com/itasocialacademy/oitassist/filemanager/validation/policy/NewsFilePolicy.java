package com.itasocialacademy.oitassist.filemanager.validation.policy;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.Set;
import org.springframework.util.unit.DataSize;

public final class NewsFilePolicy implements FilePolicy {
    public static final NewsFilePolicy INSTANCE = new NewsFilePolicy();

    private NewsFilePolicy() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AllowedExtension> getAllowedExtensions() {
        return Set.of(
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

    // TODO: confirm size limit with business
    /**
     * {@inheritDoc}
     */
    @Override
    public DataSize getMaxFileSize() {
        return DataSize.ofMegabytes(10);
    }
}
