package com.example.filerename.validation;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record ValidationIssue(Path source, String message) {

    public ValidationIssue {
        message = Objects.requireNonNull(message, "message");
        source = source == null ? null : source.toAbsolutePath().normalize();
    }

    public Optional<Path> sourcePath() {
        return Optional.ofNullable(source);
    }
}
