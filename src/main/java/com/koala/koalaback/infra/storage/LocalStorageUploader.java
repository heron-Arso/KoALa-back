package com.koala.koalaback.infra.storage;

import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 로컬 개발용 파일 저장소.
 * 파일을 로컬 디스크에 저장하고 Spring Boot 정적 리소스로 제공합니다.
 * 업로드된 파일은 {koala.storage.upload-dir}/{directory}/ 에 저장됩니다.
 * 접근 URL: {koala.cdn-base-url}/uploads/{directory}/{uuid}{ext}
 */
@Slf4j
@Component
@Profile("local")
public class LocalStorageUploader implements StorageUploader {

    @Value("${koala.storage.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${koala.cdn-base-url:http://localhost:8080}")
    private String cdnBaseUrl;

    @Override
    public String upload(MultipartFile file, String directory) {
        FileValidator.validateImageOrVideo(file);
        String key = buildKey(directory, file.getOriginalFilename());
        Path target = Paths.get(uploadDir, key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file.getBytes());
            log.info("Local upload success: path={}", target);
            return cdnBaseUrl + "/uploads/" + key;
        } catch (IOException e) {
            log.error("Local upload failed: path={}", target, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String uploadBytes(byte[] bytes, String directory, String filename, String contentType) {
        String key = buildKey(directory, filename);
        Path target = Paths.get(uploadDir, key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            log.info("Local upload bytes success: path={}", target);
            return cdnBaseUrl + "/uploads/" + key;
        } catch (IOException e) {
            log.error("Local upload bytes failed: path={}", target, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String fileUrl) {
        // /uploads/ 이후 경로를 추출해 파일 삭제
        String prefix = cdnBaseUrl + "/uploads/";
        if (!fileUrl.startsWith(prefix)) {
            log.warn("Local delete skipped — URL does not match prefix: {}", fileUrl);
            return;
        }
        String relativePath = fileUrl.substring(prefix.length());
        Path target = Paths.get(uploadDir, relativePath);
        try {
            Files.deleteIfExists(target);
            log.info("Local delete success: path={}", target);
        } catch (IOException e) {
            log.warn("Local delete failed: path={}, error={}", target, e.getMessage());
        }
    }

    private String buildKey(String directory, String originalFilename) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext  = extractExtension(originalFilename);
        return directory + "/" + uuid + ext;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
