package com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions;

import com.teamdev.fileservice.FileStorageImpl.FileStorageException;

public class KeyAlreadyExistFileStorageException extends FileStorageException {

    private String incorrectArgument;

    public KeyAlreadyExistFileStorageException(String message, String incorrectArgument) {
        super(message);
        this.incorrectArgument = incorrectArgument;
    }

    public String getIncorrectArgument() {
        return incorrectArgument;
    }
}
