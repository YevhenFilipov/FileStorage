package com.teamdev.fileservice.FileStorageImpl.FileStoragePathServiceImpl;

import com.teamdev.fileservice.FileStorageImpl.FileStoragePathService;

public class FileStoragePathServiceImpl implements FileStoragePathService {

    @Override
    public String generatePathPresentation(String key) {
        // Separates 2^30 (include positive and negative values) variants of hash code
        int cutHash = key.hashCode() % (int) Math.pow(2, 28);
        // Separates 2^15 (include positive and negative values) variants of hash code for the folders names of the first nesting level
        int fistPartHash = cutHash / (int) Math.pow(2, 14);
        // Separates 2^15 (include positive and negative values) variants of hash code for the folders names of the second nesting level
        int secondPartHash = cutHash % (int) Math.pow(2, 14);

        // In this structure we can get not more than 2^15 files for each of two nesting level
        // The total number of files to store: 2^30

        return "/" + fistPartHash + "/" + secondPartHash;
    }

    @Override
    public String generateFileNamePresentation(String key) {

        return "/" + key.replaceAll("[/ ? | > < * \\\\ : \" ]", "_");
    }
}
