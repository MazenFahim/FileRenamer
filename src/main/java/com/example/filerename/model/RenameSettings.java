package com.example.filerename.model;

import java.util.Objects;

public record RenameSettings(String prefix, int startingNumber, int padding) {

    public RenameSettings {
        prefix = Objects.requireNonNullElse(prefix, "").trim();
    }
}
