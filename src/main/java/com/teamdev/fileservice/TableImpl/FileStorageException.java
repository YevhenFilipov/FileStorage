package com.teamdev.fileservice.TableImpl;

public class FileStorageException extends Exception {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(Throwable cause) {
        super(cause);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
