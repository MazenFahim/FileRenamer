package com.example.filerename.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class RenameExecutionException extends IOException {

    private final List<String> rollbackErrors;

    public RenameExecutionException(String message, Throwable cause, List<String> rollbackErrors) {
        super(message, cause);
        this.rollbackErrors = List.copyOf(Objects.requireNonNull(rollbackErrors, "rollbackErrors"));
    }

    public List<String> rollbackErrors() {
        return rollbackErrors;
    }
}
