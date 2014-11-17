package com.teamdev.fileservice;

import com.teamdev.fileservice.FileStorageImpl.FileStorageException;

import java.io.InputStream;

public interface FileStorage {

    void saveFile(String key, InputStream inputStream) throws FileStorageException;

    void saveFile(String key, long fileLifeTime, InputStream inputStream) throws FileStorageException;

    InputStream readFile(String key) throws FileStorageException;

    void deleteFile(String key) throws FileStorageException;

    long freeSpaceInBytes();

    float freeSpaceInPercents();

    void purge(long discSpaceInBytes) throws FileStorageException;

    void purge(float discSpaceInPercents) throws FileStorageException;

}
