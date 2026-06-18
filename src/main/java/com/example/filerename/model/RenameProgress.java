package com.example.filerename.model;

import java.nio.file.Path;

public record RenameProgress(int completed, int total, Path source) {
}
