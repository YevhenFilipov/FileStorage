package com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions;

import com.teamdev.fileservice.FileStorageImpl.FileStorageException;

public class ReadWriteFileStorageException extends FileStorageException{

    Throwable cause;
    String fileKey;

    public ReadWriteFileStorageException(String message, String fileKey, Throwable cause) {
        super(message);
        this.cause = cause;
        this.fileKey = fileKey;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    public String getFileKey() {
        return fileKey;
    }
}
