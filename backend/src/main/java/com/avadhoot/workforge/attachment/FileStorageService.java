package com.avadhoot.workforge.attachment;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Abstraction over binary attachment storage. Files are never stored in the database.
 */
public interface FileStorageService {

    /**
     * Persists a stream and returns an opaque storage path/key that can later be
     * passed to {@link #load(String)}.
     */
    String store(InputStream content, String originalFilename);

    Resource load(String storagePath);

    void delete(String storagePath);
}
