package com.itasocialacademy.oitassist.filemanager.exceptions;

public class FileLocalDeleteFailureException extends RuntimeException {
    public FileLocalDeleteFailureException(String message, Exception e) {
        super(message, e);
    }
}
