package com.example.filerename.service;

import com.example.filerename.model.FileRenameContext;
import com.example.filerename.model.RenameOperation;
import com.example.filerename.model.RenamePlan;
import com.example.filerename.model.RenameSettings;
import com.example.filerename.strategy.NamingStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RenamePlanner {

    private final NamingStrategy namingStrategy;

    public RenamePlanner(NamingStrategy namingStrategy) {
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "namingStrategy");
    }

    public RenamePlan createPlan(List<Path> sourcePaths, RenameSettings settings) {
        Objects.requireNonNull(sourcePaths, "sourcePaths");
        Objects.requireNonNull(settings, "settings");

        List<Path> normalizedSources = sourcePaths.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .toList();

        Set<Path> reservedPaths = new HashSet<>(normalizedSources);
        List<RenameOperation> operations = new ArrayList<>(normalizedSources.size());

        for (int index = 0; index < normalizedSources.size(); index++) {
            Path source = normalizedSources.get(index);
            String generatedName = namingStrategy.generateName(
                    new FileRenameContext(source, settings),
                    index
            );
            Path destination = source.resolveSibling(generatedName).toAbsolutePath().normalize();
            reservedPaths.add(destination);

            Path temporaryPath = createUniqueTemporaryPath(source, reservedPaths);
            reservedPaths.add(temporaryPath);
            operations.add(new RenameOperation(source, destination, temporaryPath));
        }

        return new RenamePlan(operations);
    }

    public String previewName(Path sourcePath, RenameSettings settings, int index) {
        return namingStrategy.generateName(new FileRenameContext(sourcePath, settings), index);
    }

    private static Path createUniqueTemporaryPath(Path source, Set<Path> reservedPaths) {
        Path candidate;
        do {
            String tempName = "." + source.getFileName() + ".filerename-" + UUID.randomUUID() + ".tmp";
            candidate = source.resolveSibling(tempName).toAbsolutePath().normalize();
        } while (reservedPaths.contains(candidate) || Files.exists(candidate, LinkOption.NOFOLLOW_LINKS));
        return candidate;
    }
}
