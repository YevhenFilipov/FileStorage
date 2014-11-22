package com.teamdev.fileservice.FileStorageImpl;

import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageOperationServiceImpl.OperationServiceImpl;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.TimerTask;

public class ExpirationFilesDeleter extends TimerTask {

    private final static Logger LOGGER = Logger.getLogger(ExpirationFilesDeleter.class);
    private final FileStorageData fileStorageData;

    ExpirationFilesDeleter(FileStorageData fileStorageData) {
        this.fileStorageData = fileStorageData;
    }

    @Override
    public void run() {

        if (this.fileStorageData.expirationTimeKeySet().isEmpty())
            return;

        final Date currentTime = new Date();
        final OperationService operationService = new OperationServiceImpl();

        for (String filePath : this.fileStorageData.expirationTimeKeySet()) {
            final Long expirationTimeOfFile = this.fileStorageData.getExpirationTime(filePath);

            if (expirationTimeOfFile <= currentTime.getTime()) {
                this.fileStorageData.removeExpirationTime(filePath);

                try {
                    final long fileSize = operationService.deleteFile(filePath);
                    this.fileStorageData.decreaseTotalSizeOfFiles(fileSize);
                } catch (KeyNotExistFileStorageException e) {
                    LOGGER.info("This file not found: " + filePath, e);
                }
            }
        }
    }
}
