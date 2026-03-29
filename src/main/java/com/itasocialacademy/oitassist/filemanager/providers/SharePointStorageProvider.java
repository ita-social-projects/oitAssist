package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.io.InputStream;

public class SharePointStorageProvider implements StorageProvider {
    @Override
    public boolean supports(StorageProviderType source) {
        return false;
    }

    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        return "";
    }

    @Override
    public void deletePhysical(String filePath) {
        // Implement SharePoint deletion logic
    }
}
