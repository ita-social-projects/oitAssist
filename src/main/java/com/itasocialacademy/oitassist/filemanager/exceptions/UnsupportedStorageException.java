package com.itasocialacademy.oitassist.filemanager.exceptions;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;

public class UnsupportedStorageException extends NotFoundException {
    public UnsupportedStorageException(String message) {
        super(message, ErrorCode.PROVIDER_NOT_SUPPORTED);
    }
}