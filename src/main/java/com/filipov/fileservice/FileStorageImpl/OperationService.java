package com.filipov.fileservice.FileStorageImpl;

import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyAlreadyExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.KeyNotExistFileStorageException;
import com.filipov.fileservice.FileStorageImpl.FileStorageExceptions.NoFreeSpaceFileStorageException;

import java.io.InputStream;

/**
 * Works hard disc drive via operation system, uses java.io and java.nio
 *
 * @author Yevhen Filipov
 */

public interface OperationService {

    /**
     * Creates new folder, if it doesn't exist
     *
     * @param folderPath path of new folder
     */

    void createFolder(String folderPath);

    /**
     * Saves file to the folder
     *
     * @param filePath    path of new file
     * @param inputStream input stream, from which file will saves
     * @param freeSpace   current free space of storage (or it virtual partition)
     * @return size of saved file in bites
     * @throws NoFreeSpaceFileStorageException     if there no free space in the storage
     * @throws KeyAlreadyExistFileStorageException if file, associated with this path already exist
     */

    long saveFile(String filePath, InputStream inputStream, long freeSpace) throws NoFreeSpaceFileStorageException, KeyAlreadyExistFileStorageException;

    /**
     * Deletes file from the storage
     *
     * @param filePath path of the file
     * @return size of deleted file in bites
     * @throws KeyNotExistFileStorageException if file, associated with this path not exist
     */

    long deleteFile(String filePath) throws KeyNotExistFileStorageException;

    /**
     * Reads file from the storage
     *
     * @param filePath path of the file
     * @return Input stream of this file
     * @throws KeyNotExistFileStorageException if file, associated with this path not exist
     */

    InputStream readFile(String filePath) throws KeyNotExistFileStorageException;

    /**
     * Liberates free space in the storage to the target value in bites (or more)
     *
     * @param directoryPath          target directory path
     * @param targetDiscSpaceInBytes target free space of this directory in bites
     * @return
     */

    long purge(String directoryPath, long targetDiscSpaceInBytes);

    /**
     * Returns total size of all files in this directory, includes subdirectories
     *
     * @param path path of target directory
     * @return total size of all files in this directory, includes subdirectories
     */

    long getTotalSizeOfFiles(String path);

    /**
     * Returns free space of the partition, where this path located
     *
     * @param path path of target directory
     * @return free space of the partition, where this path located
     */

    long getFreeSpace(String path);
}
