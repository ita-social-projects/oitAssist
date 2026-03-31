package com.itasocialacademy.oitassist.filemanager.exceptions;

public class FileUploadFailureException extends RuntimeException {
    public FileUploadFailureException(String message, Exception e) {
        super(message, e);
    }
}
