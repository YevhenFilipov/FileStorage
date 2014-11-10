package com.teamdev.file_service.TableImpl;

public class FileAttribute {

    private final String filePath;
    private final long fileSize;
    private final long createDate;

    public FileAttribute(String filePath, long fileSize, long createDate) {
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.createDate = createDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getCreateDate() {
        return createDate;
    }
}
