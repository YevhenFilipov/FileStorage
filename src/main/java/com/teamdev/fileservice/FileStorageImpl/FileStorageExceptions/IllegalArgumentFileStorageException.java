package com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions;

import com.teamdev.fileservice.FileStorageImpl.FileStorageException;

public class IllegalArgumentFileStorageException extends FileStorageException {

    private final String incorrectArgument;

    public IllegalArgumentFileStorageException(String message, String incorrectArgument) {
        super(message);
        this.incorrectArgument = incorrectArgument;
    }

    public String getIncorrectArgument() {
        return incorrectArgument;
    }
}
