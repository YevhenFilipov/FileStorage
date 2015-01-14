package com.filipov.fileservice;

/**
 * This class creates specific structure of subdirectories to save 2^30 files.
 * File structure building bases on hash code of the specific key of each file.
 *
 * @author Yevhen Filipov
 */

import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;

import java.io.InputStream;

public interface FileStorage {

    /**
     * Saves the new file with specific key to the storage
     *
     * @param key         unique key of file.
     * @param inputStream input stream for this file
     * @throws NoFreeSpaceFileStorageException                                       if there no free space in the storage
     * @throws KeyAlreadyExistFileStorageException                                   if file, associated with this key already exist
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if IOException occurs
     */

    void saveFile(String key, InputStream inputStream) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException;

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

    void saveFile(String key, InputStream inputStream, long fileLifeTime) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException;

    /**
     * Reads file from the storage
     *
     * @param key specific file key
     * @return Input Stream of this file
     * @throws KeyNotExistFileStorageException                                       if the file, associated with this key doesn't exist
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if IOException occurs
     */

    InputStream readFile(String key) throws KeyNotExistFileStorageException;

    /**
     * Deletes file with specific key
     *
     * @param key specific file key
     * @throws KeyNotExistFileStorageException                                       if the file, associated with this key doesn't exist
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if IOException occurs
     */

    void deleteFile(String key) throws KeyNotExistFileStorageException;

    /**
     * Returns free space of storage in bites
     *
     * @return free space of storage in bites
     */

    long freeSpaceInBytes();

    /**
     * Returns free space of storage in percents
     *
     * @return free space of storage in percents (0..100%)
     */

    int freeSpaceInPercents();

    /**
     * Liberates free space in the storage to the target value in bites (or more)
     *
     * @param discSpaceInBytes target value of the free space
     */

    void purge(long discSpaceInBytes);

    /**
     * Liberates free space in the storage to the target value in percents (or more)
     *
     * @param discSpaceInPercents target value of the free space (0..100%)
     * @throws com.filipov.fileservice.FileStorageImpl.ReadWriteFileStorageException if no access to some stored files
     */

    void purge(int discSpaceInPercents);

}
