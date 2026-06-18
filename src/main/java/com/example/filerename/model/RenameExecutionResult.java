package com.example.filerename.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record RenameExecutionResult(int renamedCount, List<Path> finalPaths) {

    public RenameExecutionResult {
        finalPaths = List.copyOf(Objects.requireNonNull(finalPaths, "finalPaths"));
    }
}
