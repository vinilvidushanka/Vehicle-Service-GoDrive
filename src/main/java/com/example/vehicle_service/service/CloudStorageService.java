package com.example.vehicle_service.service;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class CloudStorageService {

    // bootstrap.properties එකෙන් මේ අගයන් කියවා ගනියි
    @Value("${gcp.project.id}")
    private String projectId;

    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Value("${gcp.config.file-path}")
    private String gcpConfigFile;

    public String uploadFile(MultipartFile file) throws IOException {
        // 1. JSON Key එක කියවීම (classpath: කොටස ඉවත් කර)
        String cleanPath = gcpConfigFile.replace("classpath:", "");
        InputStream keyFileStream = new ClassPathResource(cleanPath).getInputStream();

        // 2. Google Cloud Storage එකට සම්බන්ධ වීම (Authentication)
        Storage storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(keyFileStream))
                .setProjectId(projectId)
                .build()
                .getService();

        // 3. පින්තූරයට යුනික් නමක් ලබා දීම (File name duplication වළක්වා ගැනීමට)
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

        // 4. Bucket එක ඇතුළේ පින්තූරය නිර්මාණය කිරීම
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

        // 5. පින්තූරය බලන්න පුළුවන් Public URL එක ලබා දීම
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }
}