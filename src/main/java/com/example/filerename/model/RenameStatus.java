package com.example.filerename.model;

public enum RenameStatus {
    READY("Ready", "status-ready"),
    INVALID("Review", "status-invalid"),
    RENAMING("Renaming", "status-renaming"),
    COMPLETED("Completed", "status-completed"),
    FAILED("Failed", "status-failed");

    private final String label;
    private final String cssClass;

    RenameStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String label() {
        return label;
    }

    public String cssClass() {
        return cssClass;
    }
}
