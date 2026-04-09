package com.itasocialacademy.oitassist.filemanager.validation.model;

import java.util.List;

public record ValidationResult(boolean valid, List<String> violations) {
    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult fail(List<String> violations) {
        return new ValidationResult(false, violations);
    }

    public static ValidationResult fail(String violation) {
        return new ValidationResult(false, List.of(violation));
    }
}
