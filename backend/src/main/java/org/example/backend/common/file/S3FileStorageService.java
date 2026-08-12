package org.example.backend.common.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public S3FileStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String upload(MultipartFile file, String directory) {
        String fileName = createFileName(file.getOriginalFilename());
        String key = directory + "/" + fileName;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드에 실패했습니다.", e);
        }

        return "https://" + bucket + ".s3.amazonaws.com/" + key;
    }

    @Override
    public void delete(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }

    private String createFileName(String originalFileName) {
        return UUID.randomUUID() + "_" + originalFileName;
    }

    private String extractKeyFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.indexOf(".com/") + 5);
    }
}
