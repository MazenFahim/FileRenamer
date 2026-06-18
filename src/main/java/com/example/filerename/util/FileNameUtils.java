package com.example.filerename.util;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class FileNameUtils {

    private static final Pattern ILLEGAL_FILENAME_CHARACTERS =
            Pattern.compile("[<>:\"/\\\\|?*\\p{Cntrl}]");

    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

    private FileNameUtils() {
    }

    public static Optional<String> extension(Path path) {
        String filename = path.getFileName().toString();
        int lastDot = filename.lastIndexOf('.');

        if (lastDot <= 0 || lastDot == filename.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(filename.substring(lastDot + 1));
    }

    public static String extensionWithDot(Path path) {
        return extension(path).map(value -> "." + value).orElse("");
    }

    public static String stem(Path path) {
        String filename = path.getFileName().toString();
        int lastDot = filename.lastIndexOf('.');
        return lastDot <= 0 ? filename : filename.substring(0, lastDot);
    }

    public static boolean containsIllegalFilenameCharacters(String value) {
        return value != null && ILLEGAL_FILENAME_CHARACTERS.matcher(value).find();
    }

    public static boolean isReservedWindowsName(String stem) {
        if (stem == null) {
            return false;
        }
        return WINDOWS_RESERVED_NAMES.contains(stem.toUpperCase(Locale.ROOT));
    }
}
