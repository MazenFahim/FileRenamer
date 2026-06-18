package com.example.filerename.strategy;

import com.example.filerename.model.FileRenameContext;
import com.example.filerename.model.RenameSettings;
import com.example.filerename.util.FileNameUtils;

import java.util.Locale;

public final class SequentialNamingStrategy implements NamingStrategy {

    @Override
    public String generateName(FileRenameContext context, int index) {
        RenameSettings settings = context.settings();
        long sequenceNumber = (long) settings.startingNumber() + index;
        int safePadding = Math.max(1, Math.min(32, settings.padding()));
        String number = String.format(Locale.ROOT, "%0" + safePadding + "d", sequenceNumber);
        return settings.prefix() + number + FileNameUtils.extensionWithDot(context.sourcePath());
    }
}
