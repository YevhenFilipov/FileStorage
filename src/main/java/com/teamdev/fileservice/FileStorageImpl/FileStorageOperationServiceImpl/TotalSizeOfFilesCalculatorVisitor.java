package com.teamdev.fileservice.FileStorageImpl.FileStorageOperationServiceImpl;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class TotalSizeOfFilesCalculatorVisitor extends SimpleFileVisitor<Path> {

    private long totalSizeOfFiles = 0;
    private String lastAccessedFileKey;

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        this.lastAccessedFileKey = path.getFileName().toString();
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
