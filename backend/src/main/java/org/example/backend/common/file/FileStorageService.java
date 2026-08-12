package org.example.backend.common.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String upload(MultipartFile file, String directory);
    void delete(String imageUrl);
}
