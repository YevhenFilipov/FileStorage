package com.teamdev.fileservice.FileStorageImpl;

public interface FileStoragePathService {

    String generatePathPresentation(String key);

    String generateFileNamePresentation(String key);
}
