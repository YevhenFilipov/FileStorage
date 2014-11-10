package com.teamdev.file_service.TableImpl;

import com.teamdev.file_service.FileStorage;

import java.io.*;
import java.util.Date;
import java.util.List;

public class TableFileStorage implements FileStorage {

    private final FileStorageContext fileStorageContext;

    public TableFileStorage (String rootPath, long maxDiscSpace) throws FileStorageException{

        if(rootPath == null || rootPath.isEmpty())
            throw new FileStorageException("Root path expected");
        else
        fileStorageContext = new FileStorageContext(rootPath, maxDiscSpace);
    }

    @Override
    public void saveFile(String key, InputStream inputStream) throws FileStorageException {

        if(!keyIsBlank(key))
            throw new FileStorageException("Key of file is invalid");

        if(fileStorageContext.isKeyAlreadyExist(key))
            throw new FileStorageException("Some file with this key already exist");

        if(inputStream == null)
            throw new FileStorageException("Input stream Expected");


        final String filePath = fileStorageContext.getPathForNewFile() + key + ".file";
        try {
            BufferedInputStream input = new BufferedInputStream(inputStream);
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(filePath));
            int size;
            while (true) {
                size = input.read();
                if(size != -1)
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

        File currentFile = new File(filePath);
        long fileSize = currentFile.length();
        Date currentDate = new Date();
        long createDate = currentDate.getTime();

        FileAttribute fileAttribute = new FileAttribute(filePath, fileSize, createDate);
        fileStorageContext.addNewFileAttribute(key, fileAttribute);
    }

    @Override
    public void saveFile(String key, long fileLifeTime, InputStream inputStream) throws FileStorageException {

        this.saveFile(key, inputStream);
        Date currentTime  = new Date();
        long timeToDeleteFile = currentTime.getTime() + fileLifeTime;
        fileStorageContext.putLabelForTempFile(timeToDeleteFile, key);
    }

    @Override
    public InputStream readFile(String key) {
        return null;
    }

    @Override
    public void deleteFile(String key) {

        FileAttribute currentFileAttributes = fileStorageContext.getFileAttributes(key);
        String filePath = currentFileAttributes.getFilePath();
        //TODO: добавить реальное уделение из директории

        fileStorageContext.deleteFileAttribute(key);

    }

    @Override
    public long freeSpaceInBytes() {
        return fileStorageContext.getFreeDiscSpaceInBites();
    }

    @Override
    public double freeSpaceInPercents() {
        Long resultLong = this.freeSpaceInBytes()/fileStorageContext.getMaxDiscSpace() * 100;
        return resultLong.doubleValue();
    }

    @Override
    public void purge(long discSpaceInBytes) {

        List<String> keysOfOldestFiles = fileStorageContext.getKeysOfOldestFiles(discSpaceInBytes);
        for(String currentKeyOfFile: keysOfOldestFiles){
            this.deleteFile(currentKeyOfFile);
        }
    }

    @Override
    public void purge(double discSpaceInPercents) {

        Double doubleDiscSpaceInPercents = discSpaceInPercents;
        Long targetFreeSpace = doubleDiscSpaceInPercents.longValue() * fileStorageContext.getMaxDiscSpace() / 100;
        this.purge(targetFreeSpace);
    }

    private void deleteTempFiles() {
        Date currentDate = new Date();
        List<String> keysOfFilesToDelete =  fileStorageContext.getKeysForTempFilesToDelete(currentDate.getTime());
        for(String currentFileKey: keysOfFilesToDelete) {
            this.deleteFile(currentFileKey);
        }
    }

    private boolean keyIsBlank(String key) {
        for(char currentChar: key.toCharArray()) {
            switch (currentChar){
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
}
