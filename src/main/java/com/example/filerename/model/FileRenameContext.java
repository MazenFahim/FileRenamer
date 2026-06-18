package com.example.filerename.model;

import java.nio.file.Path;
import java.util.Objects;

public record FileRenameContext(Path sourcePath, RenameSettings settings) {

    public FileRenameContext {
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath").toAbsolutePath().normalize();
        settings = Objects.requireNonNull(settings, "settings");
    }
}
