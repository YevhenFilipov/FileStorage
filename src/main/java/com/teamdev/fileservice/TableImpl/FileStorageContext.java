package com.teamdev.fileservice.TableImpl;

import java.util.*;

public class FileStorageContext {

    private final String rootPath;

    private int currentLevel = 0;
    private int currentPositionInLevel = 0;
    private final long maxDiscSpace;
    private long currentSizeOfAllFiles = 0;
    private StringBuffer currentDirectoryLevelPath;
    private final Map<String, FileAttribute> pathMap = new TreeMap<String, FileAttribute>();
    private final Deque<String> pathOfDeletedFiles = new ArrayDeque<String>();
    private final Map<Long, String> tempFiles = new TreeMap<Long, String>();

    public FileStorageContext(String rootPath, long maxDiscSpace) {
        this.rootPath = rootPath;
        this.maxDiscSpace = maxDiscSpace;
        currentDirectoryLevelPath = new StringBuffer(rootPath);
    }

    public synchronized String getPathForNewFile() throws FileStorageException {

        if (this.isDiscFull())
            throw new FileStorageException("No free disc space");

        if (!pathOfDeletedFiles.isEmpty())
            return pathOfDeletedFiles.pop();
        if (pathOfDeletedFiles.isEmpty() && !isCurrentLevelDirectoryFull()) {
            currentPositionInLevel++;
            return generateNewPath();
        }
        if (pathOfDeletedFiles.isEmpty() && isCurrentLevelDirectoryFull()) {
            generateNewPathLevel();

            return generateNewPath();
        }
        return null;
    }

    public synchronized FileAttribute getFileAttributes(String fileKey) {
        return this.pathMap.get(fileKey);
    }

    private boolean isCurrentLevelDirectoryFull() {
        return currentPositionInLevel >= (Math.pow(2, currentLevel) - 1);
    }

    private String generateNewPath() {
        StringBuffer result = new StringBuffer(rootPath);
        Deque<Double> pathBuilder = new ArrayDeque<Double>();
        double tempCurrentPositionInLevel = currentPositionInLevel;
        pathBuilder.add(tempCurrentPositionInLevel);
        for (int i = 0; i < currentLevel - 1; i++) {
            if (tempCurrentPositionInLevel == 0)
                tempCurrentPositionInLevel = 1;
            tempCurrentPositionInLevel = log(2, tempCurrentPositionInLevel);
            pathBuilder.push(tempCurrentPositionInLevel);
        }
        while (!pathBuilder.isEmpty()) {
            int indicator = (int) (pathBuilder.pop() % 2);
            if (indicator == 0)
                result.append("left/");
            else
                result.append("right/");
        }
        return result.toString();
    }

    private double log(double base, double value) {
        return Math.log(value) / Math.log(base);
    }

    private void generateNewPathLevel() {
        currentLevel++;
        currentPositionInLevel = 0;
    }

    public synchronized void addNewFileAttribute(String key, FileAttribute fileAttribute) {

        this.pathMap.put(key, fileAttribute);
        this.currentSizeOfAllFiles += fileAttribute.getFileSize();

    }

    public synchronized void deleteFileAttribute(String fileKey) {

        FileAttribute fileAttribute = pathMap.remove(fileKey);
        pathOfDeletedFiles.push(fileAttribute.getFilePath());
        this.currentSizeOfAllFiles -= fileAttribute.getFileSize();

        if (tempFiles.containsValue(fileKey))
            tempFiles.remove(fileKey);

    }

    public void putLabelForTempFile(long lifeTimeOfFile, String fileKey) {
        tempFiles.put(lifeTimeOfFile, fileKey);
    }

    public synchronized List<String> getKeysForTempFilesToDelete(long currentTime) {
        List<String> result = new LinkedList<String>();
        for (Long lifetimeOfFile : tempFiles.keySet()) {
            if (!(lifetimeOfFile > currentTime))
                result.add(tempFiles.remove(lifetimeOfFile));
            else break;
        }
        return result;
    }

    private boolean isDiscFull() {
        return currentSizeOfAllFiles > maxDiscSpace;
    }

    public synchronized long getFreeDiscSpaceInBites() {
        return maxDiscSpace - currentSizeOfAllFiles;
    }

    public synchronized List<String> getKeysOfOldestFiles(long sizeOfFiles) {
        Map<Long, String> dateKeyMap = new TreeMap<Long, String>();
        for (String key : pathMap.keySet()) {
            long currentFileDate = pathMap.get(key).getCreateDate();
            dateKeyMap.put(currentFileDate, key);
        }

        List<String> result = new LinkedList<String>();
        long commonSizeOfSelectedFiles = 0;
        for (long currentFileDate : dateKeyMap.keySet()) {
            if (commonSizeOfSelectedFiles > sizeOfFiles)
                break;
            result.add(dateKeyMap.get(currentFileDate));
        }

        return result;
    }

    public synchronized long getMaxDiscSpace() {
        return maxDiscSpace;
    }

    public synchronized boolean isKeyAlreadyExist(String key) {
        return pathMap.keySet().contains(key);
    }

    public synchronized boolean isTempFilesAvailable() {
        return !tempFiles.isEmpty();
    }
}
