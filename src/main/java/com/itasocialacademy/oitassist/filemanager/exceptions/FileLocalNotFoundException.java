package com.itasocialacademy.oitassist.filemanager.exceptions;

public class FileLocalNotFoundException extends RuntimeException {
    public FileLocalNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
