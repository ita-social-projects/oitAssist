package com.itasocialacademy.oitassist.filemanager.validation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AllowedExtension {
    DOCX("docx"),
    XLSX("xlsx"),
    PPTX("pptx"),
    ACCDB("accdb"),
    JPG("jpg"),
    JPEG("jpeg"),
    PNG("png"),
    GIF("gif"),
    WEBP("webp");

    private final String rawValue;
}
