package com.filipov.fileservice.FileStorageImpl;

import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageOperationServiceImpl.OperationServiceImpl;
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
                try {
                    final long fileSize = operationService.deleteFile(filePath);
                    this.fileStorageData.decreaseTotalSizeOfFiles(fileSize);
                    this.fileStorageData.removeExpirationTime(filePath);
                } catch (KeyNotExistFileStorageException e) {
                    LOGGER.info("This file not found: " + filePath, e);
                } catch (ReadWriteFileStorageException readWriteError) {
                    LOGGER.info("Can't delete this file: " + filePath, readWriteError);
                }
            }
        }
    }
}
