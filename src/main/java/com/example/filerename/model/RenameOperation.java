package com.example.filerename.model;

import java.nio.file.Path;
import java.util.Objects;

public record RenameOperation(Path source, Path destination, Path temporaryPath) {

    public RenameOperation {
        source = normalize(source, "source");
        destination = normalize(destination, "destination");
        temporaryPath = normalize(temporaryPath, "temporaryPath");
    }

    private static Path normalize(Path path, String name) {
        return Objects.requireNonNull(path, name).toAbsolutePath().normalize();
    }
}
