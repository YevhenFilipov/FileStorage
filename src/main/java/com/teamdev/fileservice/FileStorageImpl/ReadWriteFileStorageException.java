package com.teamdev.fileservice.FileStorageImpl;

public class ReadWriteFileStorageException extends RuntimeException {

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
