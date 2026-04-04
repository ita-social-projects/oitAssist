package com.itasocialacademy.oitassist.filemanager.validation.interfaces;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import java.util.Set;

public interface FilePolicy {
    Set<AllowedExtension> getAllowedExtensions();

    int getMaxFileCount();
}
