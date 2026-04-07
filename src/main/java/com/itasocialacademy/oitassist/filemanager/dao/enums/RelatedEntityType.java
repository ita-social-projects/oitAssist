package com.itasocialacademy.oitassist.filemanager.dao.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;

@NamedInterface("RelatedEntityType")
@Getter
@RequiredArgsConstructor
public enum RelatedEntityType {
    NEWS("News"),
    TASK("Task"),
    COMPETITION("Competition"),
    SUBMISSION("Submission");

    private final String entityName;
}