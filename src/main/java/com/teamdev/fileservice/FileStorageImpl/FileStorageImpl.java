package com.teamdev.fileservice.FileStorageImpl;
/**
 * This class creates specific structure of subdirectories to save more than 1 000 000 files.
 * Binary tree used like a prototype of this file structure.
 * To make access to the files faster,
 * class uses special context, where all files attributes saves to a TreeMap.
 */

import com.teamdev.fileservice.FileStorage;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.IllegalArgumentFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.NullArgumentFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.ReadWriteFileStorageException;
import com.teamdev.fileservice.FileStorageImpl.FileStorageExceptions.UnnecessaryEventFileStorageException;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileStorageImpl implements FileStorage {

    private final Logger logger = Logger.getLogger(FileStorageImpl.class.getName());

    private final String rootPath;
    private final long maxDiscSpace;
    private long totalSizeOfFiles;
    private final NavigableMap<Long, String> tempFiles = new TreeMap<Long, String>();

    /**
     * Constructor creates new instance of class,
     * with special attributes. Attributes points to storage root path and max size of this storage
     *
     * @param rootPath     Path, where storage will be located.
     * @param maxDiscSpace max disc space in bites, which storage can be use.
     * @throws FileStorageException if root path expected
     */

    public FileStorageImpl(String rootPath, long maxDiscSpace) throws FileStorageException {
        checkRootPath(rootPath);
        checkMaxDiscSpace(rootPath, maxDiscSpace);
        this.rootPath = rootPath;
        this.maxDiscSpace = maxDiscSpace;
        Timer tempFilesDeleterTimer = new Timer(true);
        final TempFilesDeleter tempFilesDeleter = new TempFilesDeleter(this);
        tempFilesDeleterTimer.schedule(tempFilesDeleter, new Date(), 1 * 60 * 1000);

    }

    /**
     * Saves the new file with specific key to the storage
     *
     * @param key         Unique key of saved file,
     *                    it can not contain symbols:
     *                    '\'
     *                    '/'
     *                    ':'
     *                    '*'
     *                    '?'
     *                    '"'
     *                    '<'
     *                    '>'
     *                    '|'
     * @param inputStream input stream of the new file
     * @throws FileStorageException if key is invalid, or already exist.
     *                              or if input stream is null
     *                              of if there is no such disc space
     *                              or if there is a IOException
     */

    @Override
    public void saveFile(String key, InputStream inputStream) throws FileStorageException {
        if (inputStream == null)
            throw new NullArgumentFileStorageException("Input stream Expected");

        final String filePath = generatePathPresentation(key);
        try {
            checkAndCreateFolderPath(filePath);
        } catch (IllegalArgumentFileStorageException e) {
            logger.error("Can't create new folder for the file " + key, e);
            throw new ReadWriteFileStorageException("Can't create new folder for the file " + key, key, e);
        }

        File fileToSave = new File(filePath, key);

        saveFileToPath(inputStream, fileToSave, key);

        this.totalSizeOfFiles += fileToSave.length();
    }


    private void saveFileToPath(InputStream inputStream, File fileToSave, String key) throws FileStorageException {

        checkAndCreateNewFile(fileToSave, key);
        try {
            final ReadableByteChannel input = Channels.newChannel(inputStream);
            final WritableByteChannel output = Channels.newChannel(new FileOutputStream(fileToSave));

            final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

            while (input.read(buffer) != -1) {
                buffer.flip();
                output.write(buffer);
                buffer.compact();
                if (fileToSave.length() + this.totalSizeOfFiles > maxDiscSpace)
                    //If true, doesn't delete automatically!
                    throw new IllegalArgumentFileStorageException("No such free disc space to save current file", key);
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                output.write(buffer);
            }
            input.close();
            output.close();

        } catch (FileNotFoundException fileNotFoundError) {
            throw new ReadWriteFileStorageException("Current file is not accessible", key, fileNotFoundError);

        } catch (IOException iOError) {
            throw new ReadWriteFileStorageException("Can't read/write stream", key, iOError);
        }
    }

    private void checkAndCreateNewFile(File fileToSave, String key) throws IllegalArgumentFileStorageException, ReadWriteFileStorageException {
        if (fileToSave.exists())
            throw new IllegalArgumentFileStorageException("File with key " + key + " already exist", key);
        else try {
            if (!fileToSave.createNewFile())
                throw new IllegalArgumentFileStorageException("Another thread of FileStorage already creates the file with the same key", key);
        } catch (IOException iOError) {
            throw new ReadWriteFileStorageException("Can't write new file with key " + key, key, iOError);
        } catch (SecurityException securityError) {
            throw new ReadWriteFileStorageException("Can't get access to the file with key " + key, key, securityError);
        }
    }

    /**
     * Saves the new file with specific key to the storage
     *
     * @param key          Unique key of saved file,
     *                     it can not contain symbols:
     *                     '\'
     *                     '/'
     *                     ':'
     *                     '*'
     *                     '?'
     *                     '"'
     *                     '<'
     *                     '>'
     *                     '|'
     * @param inputStream  input stream of the new file
     * @param fileLifeTime lifetime period in milliseconds, after which, file will be deleted
     * @throws FileStorageException if key is invalid, or already exist.
     *                              or if input stream is null
     *                              of if there is no such disc space
     *                              or if there is a IOException
     */

    @Override
    public void saveFile(String key, long fileLifeTime, InputStream inputStream) throws FileStorageException {

        this.saveFile(key, inputStream);
        final Date currentTime = new Date();
        final long expirationTime = currentTime.getTime() + fileLifeTime;
        this.addNewTempFile(expirationTime, key);
    }

    @Override
    public InputStream readFile(String key) throws FileStorageException {

        final String filePath = this.generatePathPresentation(key);
        final File file;
        final InputStream inputStream;
        try {
            file = new File(filePath, key);
            if (!file.exists())
                throw new IllegalArgumentFileStorageException("This file doesn't exist", key);
            inputStream = new FileInputStream(file);

        } catch (FileNotFoundException fileNotFoundError) {
            throw new ReadWriteFileStorageException("Can't find the file", key, fileNotFoundError);
        } catch (SecurityException securityError) {
            throw new ReadWriteFileStorageException("File is protected from reading", key, securityError);
        }
        return inputStream;
    }

    @Override
    public void deleteFile(String key) throws FileStorageException {
        try {
            final String filePath = this.generatePathPresentation(key);
            final Path path = Paths.get(filePath + key);
            final long fileSize = (Long) Files.getAttribute(path, "size");
            if (!Files.deleteIfExists(path))
                throw new IllegalArgumentFileStorageException("This file doesn't exist", key);
            this.totalSizeOfFiles -= fileSize;

        } catch (SecurityException securityError) {
            throw new ReadWriteFileStorageException("File is protected from deleting", key, securityError);
        } catch (IOException IOError) {
            throw new ReadWriteFileStorageException("Can't get access to this file", key, IOError);
        }

    }

    @Override
    public long freeSpaceInBytes() {
        return this.totalSizeOfFiles;
    }

    @Override
    public float freeSpaceInPercents() {
        return (float) this.totalSizeOfFiles / this.maxDiscSpace * 100;
    }

    @Override
    public void purge(long discSpaceInBytes) throws FileStorageException {

        if (discSpaceInBytes <= 0)
            throw new IllegalArgumentFileStorageException("Incorrect value of target disc space", ((Long) discSpaceInBytes).toString());
        if (discSpaceInBytes > this.maxDiscSpace)
            throw new IllegalArgumentFileStorageException("Target free disc space is lager then max disc space", ((Long) discSpaceInBytes).toString());

        final Path path = Paths.get(rootPath + "userData");
        final OldestFilesFinderVisitor oldestFilesFinderVisitor = new OldestFilesFinderVisitor();
        try {
            Files.walkFileTree(path, oldestFilesFinderVisitor);
        } catch (IOException e) {
            throw new ReadWriteFileStorageException("Can't get access to some stored file", oldestFilesFinderVisitor.getLastAccessedFileKey(), e);
        }

        final NavigableMap<Long, String> sortedByModifyingTimeFiles = oldestFilesFinderVisitor.getOldestFiles();
        if (sortedByModifyingTimeFiles.isEmpty())
            throw new UnnecessaryEventFileStorageException("There is no files in storage");

        while (this.totalSizeOfFiles > discSpaceInBytes) {
            final String key = sortedByModifyingTimeFiles.remove(sortedByModifyingTimeFiles.firstKey());
            this.deleteFile(key);
        }

    }

    @Override
    public void purge(float discSpaceInPercents) throws FileStorageException {

        if (discSpaceInPercents <= 0 || discSpaceInPercents > 100)
            throw new IllegalArgumentFileStorageException("Incorrect target value of target disc space", ((Float)discSpaceInPercents).toString());

        long targetDiscSpace = (long)discSpaceInPercents * this.maxDiscSpace / 100;
        this.purge(targetDiscSpace);
    }


    private void checkKey(String key) throws IllegalArgumentFileStorageException {
        for (char currentChar : key.toCharArray()) {
            switch (currentChar) {
                case 0x5c:
                case '/':
                    throw new IllegalArgumentFileStorageException("File key is invalid", key);
            }
        }
    }

    private final class TempFilesDeleter extends TimerTask {

        private final FileStorageImpl fileStorage;

        TempFilesDeleter(FileStorageImpl fileStorage) {
            this.fileStorage = fileStorage;
        }

        @Override
        public void run() {
            logger.info("TempFilesDeleter started");

            Date currentTime = new Date();
            while (this.fileStorage.tempFiles.firstKey() <= currentTime.getTime())
                try {
                    this.fileStorage.deleteFile(this.fileStorage.tempFiles.remove(this.fileStorage.tempFiles.firstKey()));
                } catch (FileStorageException e) {
                    e.printStackTrace();
                }

            logger.info("TempFilesDeleter stopped");
        }
    }

    private void checkRootPath(String rootPath) throws IllegalArgumentFileStorageException, NullArgumentFileStorageException {
        if (rootPath == null || rootPath.isEmpty())
            throw new NullArgumentFileStorageException("Root path expected");
        checkAndCreateFolderPath(rootPath);
    }

    private void checkAndCreateFolderPath(String path) throws IllegalArgumentFileStorageException {
        File file = new File(path);

        if (!file.exists()) {
            try {
                final boolean directoryCreatesSuccessful = file.mkdirs();
                if (!directoryCreatesSuccessful)
                    logger.warn("Another FileStorage thread tries to work with the same path: " + path);
            } catch (SecurityException e) {
                throw new IllegalArgumentFileStorageException("Invalid path", path);
            }
        } else if (file.isFile())
            throw new IllegalArgumentFileStorageException("Specified root path points to a file", path);
    }

    private void checkMaxDiscSpace(String rootPath, long maxDiscSpace) throws IllegalArgumentFileStorageException {
        if (maxDiscSpace >= 0)
            throw new IllegalArgumentFileStorageException("Unnecessary value of maxDiscSpace", ((Long) maxDiscSpace).toString());

        File file = new File(rootPath);
        try {
            long freeSpace = file.getFreeSpace();
            if (maxDiscSpace > freeSpace)
                logger.error("FileStorage tries to receive " + maxDiscSpace +
                        " bites on disc, but only " + freeSpace + " bites are available!");
        } catch (SecurityException e) {
            throw new IllegalArgumentFileStorageException("Root path is not accessible", rootPath);
        }
    }

    private String generatePathPresentation(String key) throws IllegalArgumentFileStorageException {

        this.checkKey(key);
        int cutHash = key.hashCode() % (int) Math.pow(2, 28);
        int fistPartHash = cutHash / (int) Math.pow(2, 14);
        int secondPartHash = cutHash % (int) Math.pow(2, 14);

        return this.rootPath + "/" + "userData" + "/" + fistPartHash + "/" + secondPartHash;
    }

    private void addNewTempFile(long expirationTime, String key) {
        while (this.tempFiles.containsKey(expirationTime))
            expirationTime++;
        this.tempFiles.put(expirationTime, key);
    }
}
