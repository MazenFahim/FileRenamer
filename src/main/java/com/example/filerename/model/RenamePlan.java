package com.example.filerename.model;

import java.util.List;
import java.util.Objects;

public record RenamePlan(List<RenameOperation> operations) {

    public RenamePlan {
        operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    }

    public boolean isEmpty() {
        return operations.isEmpty();
    }

    public int size() {
        return operations.size();
    }
}
