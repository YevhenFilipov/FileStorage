package com.teamdev.fileservice.FileStorageImpl;

import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;

import java.io.InputStream;

public interface OperationService {

    void createFolder(String folderPath);

    long saveFile(String filePath, InputStream inputStream, long freeSpace) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException;

    long deleteFile(String filePath) throws KeyNotExistFileStorageException;

    InputStream readFile(String filePath) throws KeyNotExistFileStorageException;

    long purge(String directoryPath, long targetDiscSpaceInBytes);

    long getTotalSizeOfFiles(String path);

    long getFreeSpace(String path);
}
