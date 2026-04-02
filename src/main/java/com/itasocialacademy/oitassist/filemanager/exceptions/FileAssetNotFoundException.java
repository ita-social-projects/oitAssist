package com.itasocialacademy.oitassist.filemanager.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class FileAssetNotFoundException extends NotFoundException {
    public FileAssetNotFoundException(String message) {
        super(message, ErrorCode.FILE_ASSET_NOT_FOUND);
    }
}
