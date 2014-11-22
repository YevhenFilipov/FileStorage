package com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions;

import com.teamdev.fileservice.FileStorageImpl.FileStorageException;

public class KeyNotExistFileStorageException extends FileStorageException {

    private String incorrectArgument;

    public KeyNotExistFileStorageException(String message, String incorrectArgument) {
        super(message);
        this.incorrectArgument = incorrectArgument;
    }

    public String getIncorrectArgument() {
        return incorrectArgument;
    }
}
