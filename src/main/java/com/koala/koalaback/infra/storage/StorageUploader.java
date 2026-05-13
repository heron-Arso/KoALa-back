package com.koala.koalaback.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageUploader {
    /** 파일 업로드 후 접근 가능한 URL 반환 */
    String upload(MultipartFile file, String directory);

    /** byte[] 직접 업로드 */
    String uploadBytes(byte[] bytes, String directory, String filename, String contentType);

    /** URL로부터 파일 삭제 */
    void delete(String fileUrl);
}
