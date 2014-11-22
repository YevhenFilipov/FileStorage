package com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions;

import com.teamdev.fileservice.FileStorageImpl.FileStorageException;

public class NoFreeSpaceFileStorageException extends FileStorageException {

    private String key;

    public NoFreeSpaceFileStorageException(String message, String key) {
        super(message);
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
