package com.filipov.fileservice.FileStorageImpl.FileStorageExceptions;

public class IncorrectArgumentFileStorageException extends RuntimeException {
    public IncorrectArgumentFileStorageException(String message) {
        super(message);
    }
}
