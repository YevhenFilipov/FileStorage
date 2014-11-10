package com.teamdev.file_service.TableImpl;

import java.util.*;

public class FileStorageContext {

    private final String rootPath;

    private int numberOfCreatedFilesInDeepestDirectoryPath = 0;
    private final int maxNumberOfFilesInDirectory = 10;
    private final long maxDiscSpace;
    private long currentSizeOfAllFiles = 0;
    private StringBuffer deepestDirectoryPath;
    private final Map<String, FileAttribute> pathMap = new TreeMap<String, FileAttribute>();
    private final Deque<String> pathOfDeletedFiles = new ArrayDeque<String>();
    private final Map<Long, String> tempFiles = new TreeMap<Long, String>();

    public FileStorageContext(String rootPath, long maxDiscSpace) {
        this.rootPath = rootPath;
        this.maxDiscSpace = maxDiscSpace;
        deepestDirectoryPath = new StringBuffer(rootPath);
    }

    public String getPathForNewFile() throws FileStorageException {

        if(this.isDiscFull())
            throw new FileStorageException("No free disc space");

        if(!pathOfDeletedFiles.isEmpty())
            return pathOfDeletedFiles.pop();
        if(pathOfDeletedFiles.isEmpty() && !isDeeperDirectoryFull()) {
            numberOfCreatedFilesInDeepestDirectoryPath++;
            return deepestDirectoryPath.toString();
        }
        if(pathOfDeletedFiles.isEmpty() && isDeeperDirectoryFull()){
            return generateNewPathLevel();
        }
        return null;
    }

    public FileAttribute getFileAttributes(String fileKey){
        return this.pathMap.get(fileKey);
    }

    private boolean isDeeperDirectoryFull() {
        return numberOfCreatedFilesInDeepestDirectoryPath >= maxNumberOfFilesInDirectory;
    }

    private String generateNewPathLevel() {
        deepestDirectoryPath.append("1/");
        this.numberOfCreatedFilesInDeepestDirectoryPath = 1;
        return deepestDirectoryPath.toString();
    }

    public void addNewFileAttribute(String key, FileAttribute fileAttribute){

        this.pathMap.put(key, fileAttribute);
        this.currentSizeOfAllFiles += fileAttribute.getFileSize();

    }

    public void deleteFileAttribute(String fileKey) {

        FileAttribute fileAttribute = pathMap.remove(fileKey);
        pathOfDeletedFiles.push(fileAttribute.getFilePath());
        this.currentSizeOfAllFiles -= fileAttribute.getFileSize();

        if(tempFiles.containsValue(fileKey))
            tempFiles.remove(fileKey);

    }

    public void putLabelForTempFile (long lifeTimeOfFile, String fileKey) {
        tempFiles.put(lifeTimeOfFile, fileKey);
    }

    public List<String> getKeysForTempFilesToDelete(long currentTime){
        List<String> result = new LinkedList<String>();
        for(Long lifetimeOfFile: tempFiles.keySet()) {
            if(!(lifetimeOfFile > currentTime))
                result.add(tempFiles.remove(lifetimeOfFile));
            else break;
        }
        return result;
    }

    private boolean isDiscFull() {
        return currentSizeOfAllFiles > maxNumberOfFilesInDirectory;
    }

    public long getFreeDiscSpaceInBites() {
        return maxDiscSpace - currentSizeOfAllFiles;
    }

    public List<String> getKeysOfOldestFiles(long sizeOfFiles) {
        Map<Long, String> dateKeyMap = new TreeMap<Long, String>();
        for(String key: pathMap.keySet()) {
            long currentFileDate = pathMap.get(key).getCreateDate();
            dateKeyMap.put(currentFileDate, key);
        }

        List<String> result = new LinkedList<String>();
        long commonSizeOfSelectedFiles = 0;
        for(long currentFileDate: dateKeyMap.keySet()) {
            if(commonSizeOfSelectedFiles > sizeOfFiles)
                break;
            result.add(dateKeyMap.get(currentFileDate));
        }

        return result;
    }

    public long getMaxDiscSpace() {
        return maxDiscSpace;
    }

    public boolean isKeyAlreadyExist(String key) {
        return pathMap.keySet().contains(key);
    }
}
