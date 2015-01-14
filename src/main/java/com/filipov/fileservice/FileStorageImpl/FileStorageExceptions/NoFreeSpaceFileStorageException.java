package com.filipov.fileservice.FileStorageImpl.FileStorageExceptions;

import com.filipov.fileservice.FileStorageImpl.FileStorageException;

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
