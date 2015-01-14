package com.filipov.fileservice.FileStorageImpl;

/**
 * Generate path from any String value
 *
 * @author Yevhen Filipov
 */

public interface PathService {

    /**
     * Generate path from any String value
     *
     * @param key any string value, except {@code null}
     * @return generated path for this key
     */

    String generateFilePathPresentation(String key);
}
