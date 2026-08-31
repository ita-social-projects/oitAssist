package com.itasocialacademy.oitassist.taskassignment.api.dto;

import java.util.List;

public record TaskRequirementsDTO(
    List<RequiredFileDTO> requiredFiles) {
    public record RequiredFileDTO(
        String namingRule,
        List<String> allowedExtensions,
        Integer maxFileSizeMb) {
    }
}