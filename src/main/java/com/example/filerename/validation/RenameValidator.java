package com.example.filerename.validation;

import com.example.filerename.model.RenameOperation;
import com.example.filerename.model.RenamePlan;
import com.example.filerename.model.RenameSettings;
import com.example.filerename.util.FileNameUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RenameValidator {

    public ValidationResult validate(
            List<Path> selectedFiles,
            RenameSettings settings,
            RenamePlan plan
    ) {
        Objects.requireNonNull(selectedFiles, "selectedFiles");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(plan, "plan");

        List<ValidationIssue> issues = new ArrayList<>();

        validateSettings(settings, issues);

        if (selectedFiles.isEmpty()) {
            issues.add(new ValidationIssue(null, "Select at least one file."));
            return new ValidationResult(issues);
        }

        if (plan.operations().size() != selectedFiles.size()) {
            issues.add(new ValidationIssue(null, "The rename plan does not match the selected files."));
            return new ValidationResult(issues);
        }

        Set<Path> selected = new HashSet<>();
        for (Path path : selectedFiles) {
            Path normalized = path.toAbsolutePath().normalize();
            if (!selected.add(normalized)) {
                issues.add(new ValidationIssue(normalized, "The same file was selected more than once."));
            }
        }

        validateSources(plan, issues);
        validateGeneratedNames(plan, issues);
        validateDestinationCollisions(plan, selected, issues);

        return new ValidationResult(issues);
    }

    private static void validateSettings(RenameSettings settings, List<ValidationIssue> issues) {
        String prefix = settings.prefix();

        if (prefix.isBlank()) {
            issues.add(new ValidationIssue(null, "Prefix cannot be blank."));
        } else {
            if (FileNameUtils.containsIllegalFilenameCharacters(prefix)) {
                issues.add(new ValidationIssue(null,
                        "Prefix contains illegal filename characters: < > : \" / \\ | ? *"));
            }
            if (prefix.endsWith(".") || prefix.endsWith(" ")) {
                issues.add(new ValidationIssue(null, "Prefix cannot end with a dot or space."));
            }
        }

        if (settings.startingNumber() < 0) {
            issues.add(new ValidationIssue(null, "Starting number must be zero or greater."));
        }

        if (settings.padding() < 1 || settings.padding() > 8) {
            issues.add(new ValidationIssue(null, "Padding must be between 1 and 8 digits."));
        }
    }

    private static void validateSources(RenamePlan plan, List<ValidationIssue> issues) {
        for (RenameOperation operation : plan.operations()) {
            Path source = operation.source();

            if (!Files.exists(source)) {
                issues.add(new ValidationIssue(source, "Source file no longer exists."));
                continue;
            }

            if (!Files.isRegularFile(source)) {
                issues.add(new ValidationIssue(source, "Only regular files can be renamed."));
            }

            Path parent = source.getParent();
            if (parent == null || !Files.isWritable(parent)) {
                issues.add(new ValidationIssue(source, "The source folder is not writable."));
            }
        }
    }

    private static void validateGeneratedNames(RenamePlan plan, List<ValidationIssue> issues) {
        Map<String, RenameOperation> generatedNames = new HashMap<>();

        for (RenameOperation operation : plan.operations()) {
            String filename = operation.destination().getFileName().toString();
            String collisionKey = operation.destination().getParent().toString().toLowerCase(Locale.ROOT)
                    + "\u0000" + filename.toLowerCase(Locale.ROOT);

            RenameOperation previous = generatedNames.putIfAbsent(collisionKey, operation);
            if (previous != null) {
                issues.add(new ValidationIssue(operation.source(),
                        "Generated filename is duplicated: " + filename));
            }

            if (filename.length() > 255) {
                issues.add(new ValidationIssue(operation.source(),
                        "Generated filename exceeds 255 characters."));
            }

            if (FileNameUtils.containsIllegalFilenameCharacters(filename)) {
                issues.add(new ValidationIssue(operation.source(),
                        "Generated filename contains illegal characters."));
            }

            String stem = FileNameUtils.stem(operation.destination());
            if (FileNameUtils.isReservedWindowsName(stem)) {
                issues.add(new ValidationIssue(operation.source(),
                        "Generated filename is reserved by Windows: " + stem));
            }
        }
    }

    private static void validateDestinationCollisions(
            RenamePlan plan,
            Set<Path> selectedSources,
            List<ValidationIssue> issues
    ) {
        Map<Path, List<Path>> directoryCache = new HashMap<>();

        for (RenameOperation operation : plan.operations()) {
            Path destination = operation.destination();
            Path normalizedDestination = destination.toAbsolutePath().normalize();

            if (Files.exists(normalizedDestination, LinkOption.NOFOLLOW_LINKS) && !selectedSources.contains(normalizedDestination)) {
                issues.add(new ValidationIssue(operation.source(),
                        "Destination already exists: " + destination.getFileName()));
                continue;
            }

            Path parent = destination.getParent();
            if (parent == null || !Files.isDirectory(parent)) {
                issues.add(new ValidationIssue(operation.source(), "Destination folder is unavailable."));
                continue;
            }

            List<Path> siblings = directoryCache.computeIfAbsent(parent, RenameValidator::readDirectory);
            for (Path sibling : siblings) {
                Path normalizedSibling = sibling.toAbsolutePath().normalize();
                boolean sameNameIgnoringCase = sibling.getFileName().toString()
                        .equalsIgnoreCase(destination.getFileName().toString());

                if (sameNameIgnoringCase
                        && !selectedSources.contains(normalizedSibling)
                        && !normalizedSibling.equals(normalizedDestination)) {
                    issues.add(new ValidationIssue(operation.source(),
                            "A case-insensitive filename collision exists: " + sibling.getFileName()));
                    break;
                }
            }
        }
    }

    private static List<Path> readDirectory(Path directory) {
        List<Path> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                entries.add(entry);
            }
        } catch (IOException ignored) {
            // Source and destination checks provide the actionable validation message.
        }
        return List.copyOf(entries);
    }
}
