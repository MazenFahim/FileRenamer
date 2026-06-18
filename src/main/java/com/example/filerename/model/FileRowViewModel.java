package com.example.filerename.model;

import com.example.filerename.util.FileNameUtils;
import com.example.filerename.util.FileSizeFormatter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class FileRowViewModel {

    private final ObjectProperty<Path> path = new SimpleObjectProperty<>();
    private final ReadOnlyStringWrapper originalName = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper proposedName = new ReadOnlyStringWrapper("—");
    private final ReadOnlyStringWrapper type = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper size = new ReadOnlyStringWrapper();
    private final ObjectProperty<RenameStatus> status = new SimpleObjectProperty<>(RenameStatus.READY);
    private final ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("");

    public FileRowViewModel(Path sourcePath) {
        setPath(sourcePath);
    }

    public Path getPath() {
        return path.get();
    }

    public ObjectProperty<Path> pathProperty() {
        return path;
    }

    public void setPath(Path newPath) {
        Path normalized = Objects.requireNonNull(newPath, "newPath").toAbsolutePath().normalize();
        path.set(normalized);
        originalName.set(normalized.getFileName().toString());
        type.set(FileNameUtils.extension(normalized).map(String::toUpperCase).orElse("FILE"));
        size.set(readFormattedSize(normalized));
    }

    public String getOriginalName() {
        return originalName.get();
    }

    public ReadOnlyStringProperty originalNameProperty() {
        return originalName.getReadOnlyProperty();
    }

    public String getProposedName() {
        return proposedName.get();
    }

    public ReadOnlyStringProperty proposedNameProperty() {
        return proposedName.getReadOnlyProperty();
    }

    public void setProposedName(String value) {
        proposedName.set(Objects.requireNonNullElse(value, "—"));
    }

    public ReadOnlyStringProperty typeProperty() {
        return type.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty sizeProperty() {
        return size.getReadOnlyProperty();
    }

    public RenameStatus getStatus() {
        return status.get();
    }

    public ObjectProperty<RenameStatus> statusProperty() {
        return status;
    }

    public void setStatus(RenameStatus value) {
        status.set(Objects.requireNonNull(value, "value"));
    }

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    public void setStatusMessage(String message) {
        statusMessage.set(Objects.requireNonNullElse(message, ""));
    }

    private static String readFormattedSize(Path path) {
        try {
            return FileSizeFormatter.format(Files.size(path));
        } catch (IOException exception) {
            return "Unavailable";
        }
    }
}
