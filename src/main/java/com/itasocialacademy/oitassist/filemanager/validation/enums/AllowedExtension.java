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
    WEBP("webp"),
    MP4("mp4"),;

    private final String rawValue;
}
