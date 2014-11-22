package com.teamdev.fileservice;

import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;

import java.io.InputStream;

public interface FileStorage {

    void saveFile(String key, InputStream inputStream) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException;

    void saveFile(String key, InputStream inputStream, long fileLifeTime) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException;

    InputStream readFile(String key) throws KeyNotExistFileStorageException;

    void deleteFile(String key) throws KeyNotExistFileStorageException;

    long freeSpaceInBytes();

    float freeSpaceInPercents();

    void purge(long discSpaceInBytes);

    void purge(float discSpaceInPercents);

}
