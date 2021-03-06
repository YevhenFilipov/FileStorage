package com.filipov.fileservice.FileStorageImpl;

import com.filipov.fileservice.FileStorageImpl.FileStorageOperationServiceImpl.OperationServiceImpl;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class FileStorageData {

    private final static Logger LOGGER = Logger.getLogger(FileStorageData.class);

    private final String propertiesPath;
    private final Properties expirationFiles;
    private volatile long totalSizeOfFiles = 0;

    public FileStorageData(String userDataPath, String propertiesPath) {
        expirationFiles = new Properties();
        this.propertiesPath = propertiesPath;
        loadProperties();
        OperationService operationService = new OperationServiceImpl();
        this.totalSizeOfFiles = operationService.getTotalSizeOfFiles(userDataPath);
    }

    public long getTotalSizeOfFiles() {
        return totalSizeOfFiles;
    }

    public void increaseTotalSizeOfFiles(long fileSize) {
        this.totalSizeOfFiles += fileSize;
    }

    public void decreaseTotalSizeOfFiles(long fileSize) {
        this.totalSizeOfFiles -= fileSize;
    }

    public synchronized void putExpirationTime(String path, long expirationTime) {
        final Long expirationTimeToLong = expirationTime;
        expirationFiles.setProperty(path, expirationTimeToLong.toString());
        this.storeProperties();
    }

    public synchronized long getExpirationTime(String path) {
        final String expirationTimeString = expirationFiles.getProperty(path);
        return Long.parseLong(expirationTimeString);
    }

    public synchronized long removeExpirationTime(String path) {
        final String expirationTimeString = this.expirationFiles.remove(path).toString();
        this.storeProperties();
        return Long.parseLong(expirationTimeString);
    }

    public synchronized Set<String> expirationTimeKeySet() {
        return expirationFiles.stringPropertyNames();
    }

    public synchronized boolean isExpirationFile(String path) {
        return expirationFiles.containsKey(path);
    }

    private void storeProperties() {
        final File file = new File(propertiesPath);
        try {
            if (!file.exists())
                if (!file.createNewFile())
                    LOGGER.warn("Can't create properties file");
            expirationFiles.store(new FileWriter(file), "Path to expiration file and it expiration time");
        } catch (IOException e) {
            LOGGER.error("Can't create properties file", e);
        }

    }

    private void loadProperties() {
        final File propertiesFile = new File(this.propertiesPath);
        if (propertiesFile.exists()) try {
            expirationFiles.load(new FileReader(propertiesFile));
        } catch (IOException e) {
            LOGGER.warn("Can't get access to properties file: " + propertiesFile.getAbsolutePath(), e);
        }
    }
}
