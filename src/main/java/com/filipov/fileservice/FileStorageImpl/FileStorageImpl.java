package com.filipov.fileservice.FileStorageImpl;
/**
 * This class creates specific structure of subdirectories to save 2^30 files.
 * File structure building bases on hash code of the specific key of each file.
 *
 * @author Yevhen Filipov
 */

import com.filipov.fileservice.FileStorage;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.IncorrectArgumentFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageOperationServiceImpl.OperationServiceImpl;
import com.filipov.fileservice.FileStorageImpl.FileStoragePathServiceImpl.PathServiceImpl;
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

        if (maxDiscSpace <= 0)
            throw new IncorrectArgumentFileStorageException("Value of maxDiscSpace <= 0");

        if (rootPathFreeSpace < maxDiscSpace)
            LOGGER.warn("FileStorage tries to receive " + maxDiscSpace +
                    " bites on disc, but only " + rootPathFreeSpace + " bites are available!");

        this.maxDiscSpace = maxDiscSpace;

        Timer expirationFilesDeleterTimer = new Timer(true);
        final ExpirationFilesDeleter expirationFilesDeleter = new ExpirationFilesDeleter(this.fileStorageData);
        expirationFilesDeleterTimer.schedule(expirationFilesDeleter, new Date(), 1 * 1000l);

    }

    /**
     * Saves the new file with specific key to the storage
     *
     * @param key         unique key of file.
     * @param inputStream input stream for this file
     * @throws NoFreeSpaceFileStorageException                                       if there no free space in the storage
     * @throws KeyAlreadyExistFileStorageException                                   if file, associated with this key already exist
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if IOException occurs
     */

    @Override
    public void saveFile(String key, InputStream inputStream) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException {

        final PathService pathService = new PathServiceImpl();
        final OperationService operationService = new OperationServiceImpl();

        final String filePath = this.userDataPath + pathService.generateFilePathPresentation(key);

        try {
            final long fileSize = operationService.saveFile(filePath, inputStream, this.freeSpaceInBytes());
            fileStorageData.increaseTotalSizeOfFiles(fileSize);
        } catch (KeyAlreadyExistFileStorageException e) {
            throw new KeyAlreadyExistFileStorageException("This key already exist", key);
        }
    }

    /**
     * Saves the new expiration file with specific key to the storage. This file will be deletes automatically after fileLifeTime.
     *
     * @param key          unique key of file.
     * @param inputStream  input stream for this file
     * @param fileLifeTime expiration time of the file. After this time it'll be deletes automatically
     * @throws NoFreeSpaceFileStorageException                                       if there no free space in the storage
     * @throws KeyAlreadyExistFileStorageException                                   if file, associated with this key already exist
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if IOException occurs
     */

    @Override
    public void saveFile(String key, InputStream inputStream, long fileLifeTime) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException {

        if (fileLifeTime <= 0)
            throw new IncorrectArgumentFileStorageException("Value of fileLifeTime <= 0");
        this.saveFile(key, inputStream);
        final PathService pathService = new PathServiceImpl();
        final Date currentTime = new Date();
        final long expirationTime = currentTime.getTime() + fileLifeTime;
        final String filePath = this.userDataPath + pathService.generateFilePathPresentation(key);

        this.fileStorageData.putExpirationTime(filePath, expirationTime);
    }

    /**
     * Reads file from the storage
     *
     * @param key specific file key
     * @return Input Stream of this file
     * @throws KeyNotExistFileStorageException                                       if the file, associated with this key doesn't exist
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if IOException occurs
     */

    @Override
    public InputStream readFile(String key) throws KeyNotExistFileStorageException {

        final PathService pathService = new PathServiceImpl();
        final OperationService operationService = new OperationServiceImpl();

        final String filePath = this.userDataPath + pathService.generateFilePathPresentation(key);

        final InputStream inputStream;
        try {
            inputStream = operationService.readFile(filePath);
        } catch (KeyNotExistFileStorageException e) {
            throw new KeyNotExistFileStorageException("This key doesn't exist: " + key, key);
        }

        return inputStream;
    }

    /**
     * Deletes file with specific key
     *
     * @param key specific file key
     * @throws KeyNotExistFileStorageException                                       if the file, associated with this key doesn't exist
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if IOException occurs
     */

    @Override
    public void deleteFile(String key) throws KeyNotExistFileStorageException {

        final PathServiceImpl fileStoragePathService = new PathServiceImpl();
        final OperationService operationService = new OperationServiceImpl();

        final String filePath = this.userDataPath + fileStoragePathService.generateFilePathPresentation(key);
        final long fileSize = operationService.deleteFile(filePath);
        fileStorageData.decreaseTotalSizeOfFiles(fileSize);

        if (this.fileStorageData.isExpirationFile(filePath))
            this.fileStorageData.removeExpirationTime(filePath);
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
     * @return free space of storage in percents (0..100%)
     */

    @Override
    public int freeSpaceInPercents() {
        return (int) (this.freeSpaceInBytes() / this.maxDiscSpace * 100);
    }

    /**
     * Liberates free space in the storage to the target value in bites (or more)
     *
     * @param discSpaceInBytes target value of the free space
     */

    @Override
    public void purge(long discSpaceInBytes) {

        if (discSpaceInBytes <= 0)
            throw new IncorrectArgumentFileStorageException("Value of discSpaceInBytes <= 0");
        if (discSpaceInBytes > this.maxDiscSpace)
            discSpaceInBytes = this.maxDiscSpace;

        final OperationService operationService = new OperationServiceImpl();

        final long sizeOfFiles = operationService.purge(this.userDataPath, discSpaceInBytes - this.freeSpaceInBytes());
        fileStorageData.decreaseTotalSizeOfFiles(sizeOfFiles);
    }

    /**
     * Liberates free space in the storage to the target value in percents (or more)
     *
     * @param discSpaceInPercents target value of the free space (0..100%)
     * @throws ReadWriteFileStorageException if no access to some stored files
     */

    @Override
    public void purge(int discSpaceInPercents) {

        if (discSpaceInPercents <= 0 || discSpaceInPercents > 100)
            throw new IncorrectArgumentFileStorageException("Value of discSpaceInPercents <= 0 or > 100");

        long targetDiscSpace = discSpaceInPercents * this.maxDiscSpace / 100;
        this.purge(targetDiscSpace);
    }

}
