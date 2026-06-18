package com.example.filerename.service;

import com.example.filerename.model.RenameExecutionResult;
import com.example.filerename.model.RenameOperation;
import com.example.filerename.model.RenamePlan;
import com.example.filerename.model.RenameProgress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class RenameExecutor {

    public RenameExecutionResult execute(
            RenamePlan plan,
            Consumer<RenameProgress> progressListener
    ) throws IOException {
        Objects.requireNonNull(plan, "plan");
        Consumer<RenameProgress> listener = Objects.requireNonNullElse(progressListener, ignored -> {
        });

        if (plan.isEmpty()) {
            return new RenameExecutionResult(0, List.of());
        }

        List<RenameOperation> staged = new ArrayList<>();
        List<RenameOperation> finalized = new ArrayList<>();

        try {
            for (RenameOperation operation : plan.operations()) {
                moveWithoutOverwrite(operation.source(), operation.temporaryPath());
                staged.add(operation);
            }

            int completed = 0;
            for (RenameOperation operation : plan.operations()) {
                moveWithoutOverwrite(operation.temporaryPath(), operation.destination());
                finalized.add(operation);
                completed++;
                listener.accept(new RenameProgress(completed, plan.size(), operation.source()));
            }

            List<Path> finalPaths = plan.operations().stream()
                    .map(RenameOperation::destination)
                    .toList();
            return new RenameExecutionResult(plan.size(), finalPaths);
        } catch (IOException failure) {
            List<String> rollbackErrors = rollback(staged, finalized);
            throw new RenameExecutionException(
                    "The rename operation failed. A rollback was attempted.",
                    failure,
                    rollbackErrors
            );
        }
    }

    private static void moveWithoutOverwrite(Path source, Path destination) throws IOException {
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Destination already exists: " + destination.getFileName());
        }
        Files.move(source, destination);
    }

    private static List<String> rollback(
            List<RenameOperation> staged,
            List<RenameOperation> finalized
    ) {
        List<String> errors = new ArrayList<>();

        List<RenameOperation> reverseFinalized = new ArrayList<>(finalized);
        Collections.reverse(reverseFinalized);

        for (RenameOperation operation : reverseFinalized) {
            try {
                if (Files.exists(operation.destination(), LinkOption.NOFOLLOW_LINKS) && !Files.exists(operation.temporaryPath(), LinkOption.NOFOLLOW_LINKS)) {
                    moveWithoutOverwrite(operation.destination(), operation.temporaryPath());
                }
            } catch (IOException rollbackFailure) {
                errors.add("Could not move " + operation.destination().getFileName()
                        + " back to its temporary path: " + rollbackFailure.getMessage());
            }
        }

        List<RenameOperation> reverseStaged = new ArrayList<>(staged);
        Collections.reverse(reverseStaged);

        for (RenameOperation operation : reverseStaged) {
            try {
                if (Files.exists(operation.temporaryPath(), LinkOption.NOFOLLOW_LINKS) && !Files.exists(operation.source(), LinkOption.NOFOLLOW_LINKS)) {
                    moveWithoutOverwrite(operation.temporaryPath(), operation.source());
                }
            } catch (IOException rollbackFailure) {
                errors.add("Could not restore " + operation.source().getFileName()
                        + ": " + rollbackFailure.getMessage());
            }
        }

        return List.copyOf(errors);
    }
}
