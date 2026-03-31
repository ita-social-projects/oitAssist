package com.itasocialacademy.oitassist.filemanager.exceptions;

public class FileLocalUploadFailureException extends RuntimeException {
    public FileLocalUploadFailureException(String message, Exception e) {
        super(message, e);
    }
}
