package com.avadhoot.workforge.attachment;

import com.avadhoot.workforge.common.ErrorCode;
import com.avadhoot.workforge.config.props.StorageProperties;
import com.avadhoot.workforge.exception.BusinessException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Stores attachments on the local filesystem, partitioned by date. The returned
 * storage path is a relative key, resolved defensively against the base directory.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path root;

    public LocalFileStorageService(StorageProperties properties) {
        this.root = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Cannot initialise storage directory");
        }
    }

    @Override
    public String store(InputStream content, String originalFilename) {
        String cleaned = StringUtils.cleanPath(
                originalFilename == null ? "file" : originalFilename);
        String key = LocalDate.now() + "/" + UUID.randomUUID() + "_" + cleaned;
        Path destination = resolve(key);
        try {
            Files.createDirectories(destination.getParent());
            Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Failed to store file");
        }
        return key;
    }

    @Override
    public Resource load(String storagePath) {
        try {
            Path file = resolve(storagePath);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Attachment not found");
            }
            return resource;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Failed to read file");
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(resolve(storagePath));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_ERROR, "Failed to delete file");
        }
    }

    /** Resolve a key under the root, rejecting path traversal outside the base directory. */
    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid storage path");
        }
        return resolved;
    }
}
