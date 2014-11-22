package com.teamdev.fileservice.FileStorageImpl;
/**
 * This class creates specific structure of subdirectories to save 2^30 files.
 * File structure building bases on hash code of the specific key of each file.
 * Class have tread-safe public methods
 */

import com.teamdev.fileservice.FileStorage;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageOperationServiceImpl.OperationServiceImpl;
import com.teamdev.fileservice.FileStorageImpl.FileStoragePathServiceImpl.PathServiceImpl;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Date;
import java.util.Timer;

public class FileStorageImpl implements FileStorage {

    private final static Logger LOGGER = Logger.getLogger(FileStorageImpl.class);

    private final long maxDiscSpace;
    private final String userDataPath;
    private final FileStorageData fileStorageData;

    /**
     * Constructor creates new instance of class,
     * with special attributes. Attributes points to storage root path and max size of this storage.
     * if rootPath already have some files, it'll be reduce free space of this storage.
     * if rootPath already have some FileStorage files and configuration file, last configuration accepts automatically
     *
     * @param rootPath     path, where storage will be located.
     *                     Directory, which associated with this rootPath must be empty before the first class initialisation
     * @param maxDiscSpace max disc space in bites, which storage can be use. Value of maxDiscSpace must be  > 0
     * @throws ReadWriteFileStorageException if root path inaccessible.
     */

    public FileStorageImpl(String rootPath, long maxDiscSpace) {

        final OperationService operationService = new OperationServiceImpl();
        String propertiesFilePath = rootPath + "/FileStorage.prop";
        userDataPath = rootPath + "/userData";
        fileStorageData = new FileStorageData(userDataPath, propertiesFilePath);

        operationService.createFolder(rootPath);

        final long rootPathFreeSpace = operationService.getFreeSpace(rootPath);

        if (maxDiscSpace < 0)
            maxDiscSpace = 0;

        if (rootPathFreeSpace < maxDiscSpace)
            LOGGER.error("FileStorage tries to receive " + maxDiscSpace +
                    " bites on disc, but only " + rootPathFreeSpace + " bites are available!");

        this.maxDiscSpace = maxDiscSpace;

        Timer expirationFilesDeleterTimer = new Timer(true);
        final ExpirationFilesDeleter expirationFilesDeleter = new ExpirationFilesDeleter(this.fileStorageData);
        expirationFilesDeleterTimer.schedule(expirationFilesDeleter, new Date(), 1 * 1000);

    }

    /**
     * Saves the new file with specific key to the storage
     *
     * @param key         Unique key of file.
     *                    Key mustn't contains symbols:
     *                    '\'
     *                    '/'
     * @param inputStream input stream for this file
     * @throws ReadWriteFileStorageException if root path is read-protected
     */

    @Override
    public void saveFile(String key, InputStream inputStream) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException {

        final PathService pathService = new PathServiceImpl();
        final OperationService operationService = new OperationServiceImpl();

        final String filePath = this.userDataPath + pathService.generateDirectoryPathPresentation(key);
        final String fileName = pathService.generateFileNamePresentation(key);

        try {
            final long fileSize = operationService.saveFile(filePath + fileName, inputStream, this.freeSpaceInBytes());
            fileStorageData.increaseTotalSizeOfFiles(fileSize);
        } catch (KeyAlreadyExistFileStorageException e) {
            throw new KeyAlreadyExistFileStorageException("This key already exist", key);
        }
    }

    /**
     * Saves the new temporary file with specific key to the storage. This file will be deletes automatically after fileLifeTime.
     *
     * @param key          Unique key of file.
     *                     Key mustn't contains symbols:
     *                     '\'
     *                     '/'
     * @param inputStream  input stream for this file
     * @param fileLifeTime expiration time of the file. After this time it'll be deletes automatically
     * @throws ReadWriteFileStorageException if root path is read-protected
     */

    @Override
    public void saveFile(String key, InputStream inputStream, long fileLifeTime) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException {

        this.saveFile(key, inputStream);
        final PathService pathService = new PathServiceImpl();
        final Date currentTime = new Date();
        final long expirationTime = currentTime.getTime() + fileLifeTime;
        final String filePath = pathService.generateDirectoryPathPresentation(key) + pathService.generateFileNamePresentation(key);

        this.fileStorageData.putExpirationTime(filePath, expirationTime);
    }

    /**
     * Reads file from the storage
     *
     * @param key specified file key
     * @return Input Stream
     * @throws ReadWriteFileStorageException if file doesn't exist, or file protected from reading.
     */

    @Override
    public InputStream readFile(String key) throws KeyNotExistFileStorageException {

        final PathService pathService = new PathServiceImpl();
        final OperationService operationService = new OperationServiceImpl();

        final String filePath = this.userDataPath + pathService.generateDirectoryPathPresentation(key);
        final String fileName = pathService.generateFileNamePresentation(key);

        final InputStream inputStream;
        try {
            inputStream = operationService.readFile(filePath + fileName);
        } catch (KeyNotExistFileStorageException e) {
            throw new KeyNotExistFileStorageException("This key doesn't exist: " + key, key);
        }

        return inputStream;
    }

    /**
     * Deletes file with specific key
     *
     * @param key specific file key
     * @throws ReadWriteFileStorageException
     */

    @Override
    public void deleteFile(String key) throws KeyNotExistFileStorageException {

        final PathServiceImpl fileStoragePathService = new PathServiceImpl();
        final OperationService operationService = new OperationServiceImpl();

        final String filePath = this.userDataPath + fileStoragePathService.generateDirectoryPathPresentation(key);
        final String fileName = fileStoragePathService.generateFileNamePresentation(key);
        final long fileSize = operationService.deleteFile(filePath + fileName);
        fileStorageData.decreaseTotalSizeOfFiles(fileSize);

        if (this.fileStorageData.isExpirationFile(filePath + fileName))
            this.fileStorageData.removeExpirationTime(filePath + fileName);
    }

    /**
     * Returns free space of storage in bites
     *
     * @return free space of storage in bites
     */

    @Override
    public long freeSpaceInBytes() {
        return this.maxDiscSpace - fileStorageData.getTotalSizeOfFiles();
    }

    /**
     * Returns free space of storage in percents
     *
     * @return free space of storage in percents
     */

    @Override
    public float freeSpaceInPercents() {
        return (float) this.freeSpaceInBytes() / this.maxDiscSpace * 100;
    }

    /**
     * Liberates free space in the storage to the target value in bites (or more)
     *
     * @param discSpaceInBytes target value of the free space
     * @throws ReadWriteFileStorageException if no access to some stored files
     */

    @Override
    public void purge(long discSpaceInBytes) {

        if (discSpaceInBytes <= 0)
            return;
        if (discSpaceInBytes > this.maxDiscSpace)
            discSpaceInBytes = this.maxDiscSpace;

        final OperationService operationService = new OperationServiceImpl();

        final long sizeOfFiles = operationService.purge(this.userDataPath, discSpaceInBytes - this.freeSpaceInBytes());
        fileStorageData.decreaseTotalSizeOfFiles(sizeOfFiles);
    }

    /**
     * Liberates free space in the storage to the target value in percents (or more)
     *
     * @param discSpaceInPercents target value of the free space
     * @throws ReadWriteFileStorageException if no access to some stored files
     */

    @Override
    public void purge(float discSpaceInPercents) {

        long targetDiscSpace = (long) discSpaceInPercents * this.maxDiscSpace / 100;
        this.purge(targetDiscSpace);
    }

}
