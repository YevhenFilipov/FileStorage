package com.teamdev.fileservice.FileStorageImpl;
/**
 * This class creates specific structure of subdirectories to save 2^30 files.
 * File structure building bases on hash code of the specific key of each file.
 * Class have tread-safe public methods
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

    private final static Logger logger = Logger.getLogger(FileStorageImpl.class);
    private final static Object monitor = new Object();

    private final long maxDiscSpace;
    private final String propertiesFilePath;
    private final String userDataPath;
    private volatile long totalSizeOfFiles;
    private NavigableMap<Long, String> tempFiles;

    /**
     * Constructor creates new instance of class,
     * with special attributes. Attributes points to storage root path and max size of this storage.
     * if rootPath already have some files, it'll be reduce free space of this storage.
     * if rootPath already have some FileStorage files and configuration file, last configuration accepts automatically
     *
     * @param rootPath     path, where storage will be located.
     *                     Directory, which associated with this rootPath must be empty before the first class initialisation
     * @param maxDiscSpace max disc space in bites, which storage can be use. Value of maxDiscSpace must be  > 0
     * @throws NullArgumentFileStorageException    if root path expected, or {@code null}
     * @throws IllegalArgumentFileStorageException if root path protected from read, or rootPath points to a file, or if maxDiscSpace <= 0
     * @throws ReadWriteFileStorageException       if root path inaccessible.
     */

    public FileStorageImpl(String rootPath, long maxDiscSpace) throws IllegalArgumentFileStorageException, NullArgumentFileStorageException, ReadWriteFileStorageException {
        checkRootPath(rootPath);
        checkMaxDiscSpace(rootPath, maxDiscSpace);
        propertiesFilePath = rootPath + "/FileStorage.prop";
        File propertiesFile = new File(propertiesFilePath);
        userDataPath = rootPath + "/userData";
        final Path userStoragePath = Paths.get(userDataPath);

        synchronized (monitor) {
            if (propertiesFile.exists()) {
                this.tempFiles = this.loadTempFilesFromProperty(propertiesFile);
            } else {
                this.tempFiles = new TreeMap<Long, String>();
            }
        }

        if (Files.exists(userStoragePath))
            this.totalSizeOfFiles = this.getTotalSizeOfFiles(userStoragePath);
        else
            this.totalSizeOfFiles = 0;

        this.maxDiscSpace = maxDiscSpace;
        Timer tempFilesDeleterTimer = new Timer(true);
        final TempFilesDeleter tempFilesDeleter = new TempFilesDeleter(this);
        tempFilesDeleterTimer.schedule(tempFilesDeleter, new Date(), 1 * 1000);

    }

    /**
     * Saves the new file with specific key to the storage
     *
     * @param key         Unique key of file.
     *                    Key mustn't contains symbols:
     *                    '\'
     *                    '/'
     * @param inputStream input stream for this file
     * @throws NullArgumentFileStorageException    if inputStream or key is {@code null}
     * @throws IllegalArgumentFileStorageException if key have invalid format
     * @throws ReadWriteFileStorageException       if root path is read-protected
     */

    @Override
    public void saveFile(String key, InputStream inputStream) throws NullArgumentFileStorageException, IllegalArgumentFileStorageException, ReadWriteFileStorageException {
        if (inputStream == null)
            throw new NullArgumentFileStorageException("Input stream expected");

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

    private void saveFileToPath(InputStream inputStream, File fileToSave, String key) throws ReadWriteFileStorageException, IllegalArgumentFileStorageException {

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
     * Saves the new temporary file with specific key to the storage. This file will be deletes automatically after fileLifeTime.
     *
     * @param key          Unique key of file.
     *                     Key mustn't contains symbols:
     *                     '\'
     *                     '/'
     * @param inputStream  input stream for this file
     * @param fileLifeTime expiration time of the file. After this time it'll be deletes automatically
     * @throws NullArgumentFileStorageException    if inputStream or key is {@code null}
     * @throws IllegalArgumentFileStorageException if key have invalid format
     * @throws ReadWriteFileStorageException       if root path is read-protected
     */

    @Override
    public void saveFile(String key, InputStream inputStream, long fileLifeTime) throws ReadWriteFileStorageException, IllegalArgumentFileStorageException, NullArgumentFileStorageException {

        this.saveFile(key, inputStream);
        final Date currentTime = new Date();
        final long expirationTime = currentTime.getTime() + fileLifeTime;
        this.addNewTempFile(expirationTime, key);
        final File sourcePropertiesFile = new File(propertiesFilePath);
        Properties properties = this.getLastSavedProperties(sourcePropertiesFile);
        properties.setProperty(((Long) expirationTime).toString(), key);
        this.saveProperties(properties);

    }

    /**
     * Reads file from the storage
     *
     * @param key specified file key
     * @return Input Stream
     * @throws IllegalArgumentFileStorageException if key is invalid.
     * @throws ReadWriteFileStorageException       if file doesn't exist, or file protected from reading.
     * @throws NullArgumentFileStorageException    if key is {@code null}
     */

    @Override
    public InputStream readFile(String key) throws IllegalArgumentFileStorageException, ReadWriteFileStorageException, NullArgumentFileStorageException {

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
            throw new ReadWriteFileStorageException("File protected from reading", key, securityError);
        }
        return inputStream;
    }

    /**
     * Deletes file with specific key
     *
     * @param key specific file key
     * @throws IllegalArgumentFileStorageException if key is invalid
     * @throws ReadWriteFileStorageException
     * @throws NullArgumentFileStorageException    if key is {@code null}
     */

    @Override
    public void deleteFile(String key) throws IllegalArgumentFileStorageException, ReadWriteFileStorageException, NullArgumentFileStorageException {
        try {
            final String filePath = this.generatePathPresentation(key);
            final Path path = Paths.get(filePath + "/" + key);
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

    /**
     * Returns free space of storage in bites
     *
     * @return free space of storage in bites
     */

    @Override
    public long freeSpaceInBytes() {
        return this.maxDiscSpace - this.totalSizeOfFiles;
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
     * @throws IllegalArgumentFileStorageException  if discSpaceInBytes is invalid
     * @throws ReadWriteFileStorageException        if no access to some stored files
     * @throws UnnecessaryEventFileStorageException if target value of the free space less than current free space of storage
     * @throws NullArgumentFileStorageException     if discSpaceInBytes is {@code null}
     */

    @Override
    public void purge(long discSpaceInBytes) throws IllegalArgumentFileStorageException, ReadWriteFileStorageException, UnnecessaryEventFileStorageException, NullArgumentFileStorageException {

        if (discSpaceInBytes <= 0)
            throw new IllegalArgumentFileStorageException("Incorrect value of target disc space", ((Long) discSpaceInBytes).toString());
        if (discSpaceInBytes > this.maxDiscSpace)
            throw new IllegalArgumentFileStorageException("Target free disc space is lager then max disc space", ((Long) discSpaceInBytes).toString());

        final Path path = Paths.get(userDataPath);
        final OldestFilesFinderVisitor oldestFilesFinderVisitor = new OldestFilesFinderVisitor();
        try {
            Files.walkFileTree(path, oldestFilesFinderVisitor);
        } catch (IOException e) {
            throw new ReadWriteFileStorageException("Can't get access to some stored file", oldestFilesFinderVisitor.getLastAccessedFileKey(), e);
        }

        final NavigableMap<Long, String> sortedByModifyingTimeFiles = oldestFilesFinderVisitor.getOldestFiles();
        if (sortedByModifyingTimeFiles.isEmpty())
            throw new UnnecessaryEventFileStorageException("There is no files in storage");

        while (this.freeSpaceInBytes() < discSpaceInBytes && this.freeSpaceInBytes() > 0) {
            final String key = sortedByModifyingTimeFiles.remove(sortedByModifyingTimeFiles.firstKey());
            this.deleteFile(key);
        }

    }

    /**
     * Liberates free space in the storage to the target value in percents (or more)
     *
     * @param discSpaceInPercents target value of the free space
     * @throws IllegalArgumentFileStorageException  if discSpaceInPercents is invalid
     * @throws ReadWriteFileStorageException        if no access to some stored files
     * @throws UnnecessaryEventFileStorageException if target value of the free space less than current free space of storage
     * @throws NullArgumentFileStorageException     if discSpaceInPercents is {@code null}
     */

    @Override
    public void purge(float discSpaceInPercents) throws ReadWriteFileStorageException, IllegalArgumentFileStorageException, UnnecessaryEventFileStorageException, NullArgumentFileStorageException {

        if (discSpaceInPercents <= 0 || discSpaceInPercents > 100)
            throw new IllegalArgumentFileStorageException("Incorrect target value of target disc space", ((Float) discSpaceInPercents).toString());

        long targetDiscSpace = (long) discSpaceInPercents * this.maxDiscSpace / 100;
        this.purge(targetDiscSpace);
    }


    private void checkKey(String key) throws IllegalArgumentFileStorageException, NullArgumentFileStorageException {

        if (key == null) {
            throw new NullArgumentFileStorageException("Key is null");
        }
        for (char currentChar : key.toCharArray()) {
            switch (currentChar) {
                case 0x5c:
                case '/':
                    throw new IllegalArgumentFileStorageException("File key is invalid", key);
            }
        }
    }

    /**
     * This class runs automatically
     * and every 1 * 1000 milliseconds finds temporary files
     * which have to delete and deletes them.
     */

    private final class TempFilesDeleter extends TimerTask {

        private final FileStorageImpl fileStorage;

        TempFilesDeleter(FileStorageImpl fileStorage) {
            this.fileStorage = fileStorage;
        }

        @Override
        public void run() {
            logger.info("TempFilesDeleter started");

            synchronized (monitor) {
                if (this.fileStorage.tempFiles.isEmpty())
                    return;

                Date currentTime = new Date();
                while (this.fileStorage.tempFiles.firstKey() <= currentTime.getTime())
                    try {
                        final Long expirationTimeOfFile = this.fileStorage.tempFiles.firstKey();
                        final String keyOfFileToDelete = this.fileStorage.tempFiles.remove(expirationTimeOfFile);
                        File fileToDelete = new File(this.fileStorage.generatePathPresentation(keyOfFileToDelete) + "/" + keyOfFileToDelete);
                        if (fileToDelete.exists()) {
                            this.fileStorage.deleteFile(keyOfFileToDelete);
                        }
                        this.fileStorage.tempFiles.remove(expirationTimeOfFile);
                    } catch (FileStorageException e) {
                        e.printStackTrace();
                    }
            }

            logger.info("TempFilesDeleter stopped");
        }
    }

    private void checkRootPath(String rootPath) throws IllegalArgumentFileStorageException, NullArgumentFileStorageException, ReadWriteFileStorageException {
        if (rootPath == null || rootPath.isEmpty())
            throw new NullArgumentFileStorageException("Root path expected");
        checkAndCreateFolderPath(rootPath);
    }

    private void checkAndCreateFolderPath(String path) throws IllegalArgumentFileStorageException, ReadWriteFileStorageException {
        File file = new File(path);

        if (!file.exists()) {
            try {
                final boolean directoryCreatesSuccessful = file.mkdirs();
                if (!directoryCreatesSuccessful)
                    logger.warn("Another FileStorage thread tries to work with the same path: " + path);
            } catch (SecurityException e) {
                throw new ReadWriteFileStorageException("This path read-protected", path, e);
            }
        } else if (file.isFile())
            throw new IllegalArgumentFileStorageException("Specified root path points to a file", path);
    }

    private void checkMaxDiscSpace(String rootPath, long maxDiscSpace) throws IllegalArgumentFileStorageException, ReadWriteFileStorageException {
        if (maxDiscSpace <= 0)
            throw new IllegalArgumentFileStorageException("Unnecessary value of maxDiscSpace", ((Long) maxDiscSpace).toString());

        File file = new File(rootPath);
        try {
            long freeSpace = file.getFreeSpace();
            if (maxDiscSpace > freeSpace)
                logger.error("FileStorage tries to receive " + maxDiscSpace +
                        " bites on disc, but only " + freeSpace + " bites are available!");
        } catch (SecurityException e) {
            throw new ReadWriteFileStorageException("Root path is not accessible", rootPath, e);
        }
    }

    private String generatePathPresentation(String key) throws IllegalArgumentFileStorageException, NullArgumentFileStorageException {

        this.checkKey(key);
        // Separates 2^30 (include positive and negative values) variants of hash code
        int cutHash = key.hashCode() % (int) Math.pow(2, 28);
        // Separates 2^15 (include positive and negative values) variants of hash code for the folders names of the first nesting level
        int fistPartHash = cutHash / (int) Math.pow(2, 14);
        // Separates 2^15 (include positive and negative values) variants of hash code for the folders names of the second nesting level
        int secondPartHash = cutHash % (int) Math.pow(2, 14);

        // In this structure we can get not more than 2^15 files for each of two nesting level
        // The total number of files to store: 2^30
        return this.userDataPath + "/" + fistPartHash + "/" + secondPartHash;
    }

    private void addNewTempFile(Long expirationTime, String key) throws IllegalArgumentFileStorageException, ReadWriteFileStorageException {
        Properties properties = new Properties();
        synchronized (monitor) {
            for (Long expirationTimeOfCurrentFile : this.tempFiles.keySet()) {
                if (this.tempFiles.get(expirationTimeOfCurrentFile).equals(key))
                    this.tempFiles.remove(expirationTimeOfCurrentFile);
                else
                    properties.setProperty(expirationTimeOfCurrentFile.toString(), this.tempFiles.get(expirationTimeOfCurrentFile));
            }
            while (this.tempFiles.containsKey(expirationTime))
                expirationTime++;

            this.tempFiles.put(expirationTime, key);
        }
        properties.setProperty(expirationTime.toString(), key);
        this.saveProperties(properties);
    }

    private NavigableMap<Long, String> loadTempFilesFromProperty(File sourceFile) throws ReadWriteFileStorageException {
        final Properties properties = getLastSavedProperties(sourceFile);
        NavigableMap<Long, String> resultMap = new TreeMap<Long, String>();
        for (String expirationTimeString : properties.stringPropertyNames()) {
            Long expirationTimeLong = Long.parseLong(expirationTimeString);
            resultMap.put(expirationTimeLong, properties.getProperty(expirationTimeString));
        }
        return resultMap;
    }

    private Properties getLastSavedProperties(File sourcePropertiesFile) throws ReadWriteFileStorageException {
        final Properties properties = new Properties();
        try {
            properties.load(new FileReader(sourcePropertiesFile));
        } catch (IOException e) {
            throw new ReadWriteFileStorageException("Can't load properties from file", propertiesFilePath, e);
        }
        return properties;
    }

    private void saveProperties(Properties properties) throws ReadWriteFileStorageException {
        File propertiesFile = new File(propertiesFilePath);
        try {
            properties.store(new FileWriter(propertiesFile), "FileStorage, map of temporary files. ExpirationTime: Key");
        } catch (IOException e) {
            throw new ReadWriteFileStorageException("Can't get access to properties file", propertiesFile.getName(), e);
        }
    }

    private long getTotalSizeOfFiles(Path userStoragePath) throws ReadWriteFileStorageException {

        final TotalSizeOfFilesCalculatorVisitor totalSizeOfFilesCalculatorVisitor = new TotalSizeOfFilesCalculatorVisitor();
        try {
            Files.walkFileTree(userStoragePath, totalSizeOfFilesCalculatorVisitor);
        } catch (IOException e) {
            throw new ReadWriteFileStorageException("Can't get access to some stored file", totalSizeOfFilesCalculatorVisitor.getLastAccessedFileKey(), e);
        }

        return totalSizeOfFilesCalculatorVisitor.getTotalSizeOfFiles();
    }

}
