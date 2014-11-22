package com.teamdev.fileservice.FileStorageImpl.FileStorageOperationServiceImpl;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.NavigableMap;
import java.util.TreeMap;

public class OldestFilesFinderVisitor extends SimpleFileVisitor<Path> {

    private final NavigableMap<Long, String> oldestFiles;
    private String lastAccessedFileKey;

    public OldestFilesFinderVisitor() {
        this.oldestFiles = new TreeMap<Long, String>();
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        final String filePath = path.toString();
        this.lastAccessedFileKey = filePath;
        FileTime fileTime = (FileTime) Files.getAttribute(path, "lastModifiedTime");
        final Long lastModifiedTimeOfFile = fileTime.toMillis();
        this.oldestFiles.put(lastModifiedTimeOfFile, filePath);
        return FileVisitResult.CONTINUE;
    }

    public NavigableMap<Long, String> getOldestFiles() {
        return oldestFiles;
    }

    public String getLastAccessedFileKey() {
        return lastAccessedFileKey;
    }
}
