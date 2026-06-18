package com.example.filerename.strategy;

import com.example.filerename.model.FileRenameContext;

public interface NamingStrategy {
    String generateName(FileRenameContext context, int index);
}
