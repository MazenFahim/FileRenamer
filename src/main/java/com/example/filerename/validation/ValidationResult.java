package com.example.filerename.validation;

import java.util.List;
import java.util.Objects;

public record ValidationResult(List<ValidationIssue> issues) {

    public ValidationResult {
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }

    public boolean isValid() {
        return issues.isEmpty();
    }

    public List<String> messages() {
        return issues.stream().map(ValidationIssue::message).toList();
    }
}
