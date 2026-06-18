package com.example.filerename.util;

import java.util.Locale;

public final class FileSizeFormatter {

    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

    private FileSizeFormatter() {
    }

    public static String format(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < UNITS.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        return String.format(Locale.ROOT, "%.1f %s", value, UNITS[unitIndex]);
    }
}
