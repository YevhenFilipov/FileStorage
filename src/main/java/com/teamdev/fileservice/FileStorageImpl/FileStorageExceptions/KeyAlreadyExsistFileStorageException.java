package com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions;

public class KeyAlreadyExsistFileStorageException extends IllegalArgumentFileStorageException{
    public KeyAlreadyExsistFileStorageException(String message, String incorrectArgument) {
        super(message, incorrectArgument);
    }
}
