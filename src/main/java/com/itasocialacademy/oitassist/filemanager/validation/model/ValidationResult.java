package com.itasocialacademy.oitassist.filemanager.validation.model;

import java.util.List;

public record ValidationResult(boolean valid, List<String> violations) {
    /**
     * Creates a successful validation result with no violations.
     *
     * @return a valid {@link ValidationResult}
     */
    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    /**
     * Creates a failed validation result with the given list of violations.
     *
     * @param violations the constraint violation messages
     * @return an invalid {@link ValidationResult}
     */
    public static ValidationResult fail(List<String> violations) {
        return new ValidationResult(false, violations);
    }

    /**
     * Creates a failed validation result with a single violation message.
     *
     * @param violation the constraint violation message
     * @return an invalid {@link ValidationResult}
     */
    public static ValidationResult fail(String violation) {
        return new ValidationResult(false, List.of(violation));
    }
}
