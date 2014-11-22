package com.teamdev.fileservice.FileStorageImpl.FileStorageOperationServiceImpl;

import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.OperationService;
import com.teamdev.fileservice.FileStorageImpl.ReadWriteFileStorageException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NavigableMap;

public class OperationServiceImpl implements OperationService {

    private final static org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(OperationServiceImpl.class);

    @Override
    public void createFolder(String folderPath) {

        File file = new File(folderPath);

        if (!file.exists())
            if (!file.mkdirs())
                LOGGER.warn("Can't create new folder: " + folderPath);

    }

    @Override
    public long saveFile(String filePath, InputStream inputStream, long freeSpace) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException {

        File file = new File(filePath);

        if (file.exists()) {
            throw new KeyAlreadyExistFileStorageException("This key already exist", file.getAbsolutePath());
        }

        this.createFolder(file.getParent());

        try {
            if (!file.createNewFile()) {
                LOGGER.warn("Can't create new file: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new ReadWriteFileStorageException("Can't get access to file", file.getAbsolutePath(), e);
        }

        try {
            final ReadableByteChannel input = Channels.newChannel(inputStream);
            final WritableByteChannel output = Channels.newChannel(new FileOutputStream(file));

            final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

            while (input.read(buffer) != -1) {
                buffer.flip();
                output.write(buffer);
                buffer.compact();
                if (file.length() > freeSpace)
                    //If true, file won't deletes automatically!
                    throw new NoFreeSpaceFileStorageException("No such free disc space to save current file", file.getAbsolutePath());
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                output.write(buffer);
            }
            input.close();
            output.close();

        } catch (IOException iOError) {
            throw new ReadWriteFileStorageException("Can't read/write stream", file.getAbsolutePath(), iOError);
        }

        return file.length();
    }

    @Override
    public long deleteFile(String filePath) throws KeyNotExistFileStorageException {

        final Path path = Paths.get(filePath);
        final long fileSize;
        try {
            fileSize = (Long) Files.getAttribute(path, "size");
            if (!Files.deleteIfExists(path))
                throw new KeyNotExistFileStorageException("This file doesn't exist", path.toString());
        } catch (IOException IOError) {
            throw new ReadWriteFileStorageException("Can't get access to this file", path.toString(), IOError);
        }
        return fileSize;
    }

    @Override
    public InputStream readFile(String filePath) throws KeyNotExistFileStorageException {

        final File file;
        final InputStream inputStream;
        try {
            file = new File(filePath);
            if (!file.exists())
                throw new KeyNotExistFileStorageException("This file doesn't exist", file.getAbsolutePath());
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new ReadWriteFileStorageException("Can't get access to the file", filePath, e);
        }
        return inputStream;
    }

    @Override
    public long purge(String directoryPath, long purgeDiscSpaceInBytes) {
        final Path path = Paths.get(directoryPath);
        final OldestFilesFinderVisitor oldestFilesFinderVisitor = new OldestFilesFinderVisitor();
        try {
            Files.walkFileTree(path, oldestFilesFinderVisitor);
        } catch (IOException e) {
            throw new ReadWriteFileStorageException("Can't get access to some stored file", oldestFilesFinderVisitor.getLastAccessedFileKey(), e);
        }

        final NavigableMap<Long, String> sortedByModifyingTimeFiles = oldestFilesFinderVisitor.getOldestFiles();

        long sizeOfDeletedFiles = 0;
        while (sizeOfDeletedFiles < purgeDiscSpaceInBytes && !sortedByModifyingTimeFiles.isEmpty()) {
            final String filePath = sortedByModifyingTimeFiles.remove(sortedByModifyingTimeFiles.firstKey());
            try {
                sizeOfDeletedFiles += this.deleteFile(filePath);
            } catch (KeyNotExistFileStorageException e) {
                throw new ReadWriteFileStorageException("Can't delete file", filePath, e);
            }
        }
        return sizeOfDeletedFiles;
    }

    public long getTotalSizeOfFiles(String path) {

        final Path targetPath = Paths.get(path);

        final TotalSizeOfFilesCalculatorVisitor totalSizeOfFilesCalculatorVisitor = new TotalSizeOfFilesCalculatorVisitor();
        try {
            Files.walkFileTree(targetPath, totalSizeOfFilesCalculatorVisitor);
        } catch (IOException e) {
            return 0;
            //throw new ReadWriteFileStorageException("Can't get access to some storage files", totalSizeOfFilesCalculatorVisitor.getLastAccessedFileKey(), e);
        }
        return totalSizeOfFilesCalculatorVisitor.getTotalSizeOfFiles();
    }

    public long getFreeSpace(String path) {
        File file = new File(path);
        return file.getFreeSpace();
    }

}
