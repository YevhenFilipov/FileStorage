package com.teamdev.file_service.TableImpl;
/**
 * This class creates specific structure of subdirectories to save more than 1 000 000 files.
 * Binary tree used like a prototype of this file structure.
 * To make access to the files faster,
 * class uses special context, where all files attributes saves to a TreeMap.
 */

import com.teamdev.file_service.FileStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;
import java.util.List;

public class TableFileStorage implements FileStorage {

    private final FileStorageContext fileStorageContext;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Constructor creates new instance of class,
     * with special attributes. Attributes points to storage root path and max size of this storage
     *
     * @param rootPath     Path, where storage will be located.
     * @param maxDiscSpace max disc space in bites, which storage can be use.
     * @throws FileStorageException if root path expected
     */

    public TableFileStorage(String rootPath, long maxDiscSpace) throws FileStorageException {

        if (rootPath == null || rootPath.isEmpty())
            throw new FileStorageException("Root path expected");
        else
            fileStorageContext = new FileStorageContext(rootPath, maxDiscSpace);
        new TempFilesCleaner(this).start();
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

        tableFileStorageParametersValidation(key, inputStream);

        final String filePath = fileStorageContext.getPathForNewFile();

        saveFileToPath(inputStream, filePath, key);

        File currentFile = new File(filePath, key);
        long fileSize = currentFile.length();
        Date currentDate = new Date();
        long createDate = currentDate.getTime();

        FileAttribute fileAttribute = new FileAttribute(filePath, fileSize, createDate);
        fileStorageContext.addNewFileAttribute(key, fileAttribute);

        if (fileStorageContext.getFreeDiscSpaceInBites() <= 0) {
            this.deleteFile(key);
            throw new FileStorageException("No such disc space.");
        }
    }

    private void tableFileStorageParametersValidation(String key, InputStream inputStream) throws FileStorageException {
        if (!keyIsBlank(key))
            throw new FileStorageException("Key of file is invalid");

        if (fileStorageContext.isKeyAlreadyExist(key))
            throw new FileStorageException("Some file with this key already exist");

        if (inputStream == null)
            throw new FileStorageException("Input stream Expected");
    }

    private void saveFileToPath(InputStream inputStream, String filePath, String keyOfFile) throws FileStorageException {
        try {
            BufferedInputStream input = new BufferedInputStream(inputStream);
            File pathOfFile = new File(filePath);
            final boolean mkdirs = pathOfFile.mkdirs();
            File fileToSave = new File(pathOfFile, keyOfFile);
            fileToSave.createNewFile();
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(fileToSave));
            int size;
            while (true) {
                size = input.read();
                if (size != -1)
                    output.write(size);
                else break;
            }
            input.close();
            output.flush();
            output.close();
        } catch (FileNotFoundException e) {
            throw new FileStorageException("Can't save new file", e);
        } catch (IOException iOError) {
            throw new FileStorageException("Can't read/write stream", iOError);
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
        Date currentTime = new Date();
        long timeToDeleteFile = currentTime.getTime() + fileLifeTime;
        fileStorageContext.putLabelForTempFile(timeToDeleteFile, key);

    }

    @Override
    public InputStream readFile(String key) throws FileStorageException {

        if (!fileStorageContext.isKeyAlreadyExist(key))
            throw new FileStorageException("This key doesn't exist");

        String filePath = fileStorageContext.getFileAttributes(key).getFilePath();
        InputStream inputStream;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(filePath + key));
        } catch (FileNotFoundException e) {
            throw new FileStorageException("File not found", e);
        }
        return inputStream;
    }

    @Override
    public void deleteFile(String key) throws FileStorageException {

        FileAttribute currentFileAttributes = fileStorageContext.getFileAttributes(key);
        String filePath = currentFileAttributes.getFilePath();

        File file = new File(filePath, key);
        boolean success = file.delete();
        if (success)
            fileStorageContext.deleteFileAttribute(key);
        else throw new FileStorageException("Can't delete this file");

    }

    @Override
    public long freeSpaceInBytes() {
        return fileStorageContext.getFreeDiscSpaceInBites();
    }

    @Override
    public double freeSpaceInPercents() {

        final double freeDiscSpace = (double) this.freeSpaceInBytes();
        final double maxDiscSpace = (double) fileStorageContext.getMaxDiscSpace();
        Double result = freeDiscSpace / maxDiscSpace * 100;
        return result;
    }

    @Override
    public void purge(long discSpaceInBytes) throws FileStorageException {

        List<String> keysOfOldestFiles = fileStorageContext.getKeysOfOldestFiles(discSpaceInBytes);
        for (String currentKeyOfFile : keysOfOldestFiles) {
            this.deleteFile(currentKeyOfFile);
        }
    }

    @Override
    public void purge(double discSpaceInPercents) throws FileStorageException {

        Double doubleDiscSpaceInPercents = discSpaceInPercents;
        Long targetFreeSpace = doubleDiscSpaceInPercents.longValue() * fileStorageContext.getMaxDiscSpace() / 100;
        this.purge(targetFreeSpace);
    }

    private void deleteTempFiles() throws FileStorageException {
        Date currentDate = new Date();
        List<String> keysOfFilesToDelete = fileStorageContext.getKeysForTempFilesToDelete(currentDate.getTime());
        if (keysOfFilesToDelete.isEmpty()) return;
        for (String currentFileKey : keysOfFilesToDelete) {
            this.deleteFile(currentFileKey);
        }
    }

    private boolean keyIsBlank(String key) {
        for (char currentChar : key.toCharArray()) {
            switch (currentChar) {
                case 0x5c:
                case '/':
                case ':':
                case '*':
                case '?':
                case '"':
                case '<':
                case '>':
                case '|':
                    return false;
            }
        }
        return true;
    }

    private final class TempFilesCleaner extends Thread {

        private final Thread parentThread;

        private final TableFileStorage tableFileStorage;

        protected TempFilesCleaner(TableFileStorage tableFileStorage) {
            this.tableFileStorage = tableFileStorage;
            parentThread = Thread.currentThread();
        }

        @Override
        public void run() {
            logger.info("TempFilesCleaner started");

            while (parentThread.isAlive()) {
                try {
                    sleep(1 * 1000);
                    this.tableFileStorage.deleteTempFiles();
                } catch (InterruptedException e) {
                    continue;
                } catch (FileStorageException e) {
                    continue;
                }
            }
            logger.info("TempFilesCleaner stopped");
        }
    }
}
