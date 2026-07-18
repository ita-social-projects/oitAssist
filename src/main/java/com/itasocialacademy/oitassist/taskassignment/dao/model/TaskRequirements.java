package com.itasocialacademy.oitassist.taskassignment.dao.model;

import java.util.List;

public record TaskRequirements(
    List<RequiredFile> requiredFileList) {
    public record RequiredFile(
        String prompt,
        String namingRule,
        List<String> allowedExtensions,
        Integer maxFileSizeMb) {
    }
}
