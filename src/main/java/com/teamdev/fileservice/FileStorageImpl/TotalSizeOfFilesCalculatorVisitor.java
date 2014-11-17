package com.teamdev.fileservice.FileStorageImpl;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TotalSizeOfFilesCalculatorVisitor extends SimpleFileVisitor<Path> {

    private long totalSizeOfFiles = 0;
    private String lastAccessedFileKey;

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        final String key = path.getFileName().toString();
        this.lastAccessedFileKey = key;
        final long fileSize = (Long) Files.getAttribute(path, "size");
        this.totalSizeOfFiles += fileSize;
            return FileVisitResult.CONTINUE;
    }

    public long getTotalSizeOfFiles() {
        return totalSizeOfFiles;
    }

    public String getLastAccessedFileKey() {
        return lastAccessedFileKey;
    }
}
