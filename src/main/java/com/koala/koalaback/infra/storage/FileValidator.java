package com.koala.koalaback.infra.storage;

import com.koala.koalaback.global.exception.BusinessException;
import com.koala.koalaback.global.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * 파일 업로드 보안 검증 유틸
 *
 * Content-Type 헤더는 클라이언트가 임의로 조작 가능하므로
 * 실제 파일 바이너리의 매직바이트(magic bytes)를 읽어 형식을 검증합니다.
 */
public class FileValidator {

    // ── 파일 크기 제한 ─────────────────────────────────────
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;   // 10 MB
    private static final long MAX_VIDEO_SIZE = 500L * 1024 * 1024;  // 500 MB

    // ── 이미지 매직바이트 시그니처 ─────────────────────────
    private static final byte[] JPEG_MAGIC  = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
    private static final byte[] PNG_MAGIC   = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61}; // GIF87a
    private static final byte[] GIF89_MAGIC = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}; // GIF89a
    private static final byte[] WEBP_RIFF   = {0x52, 0x49, 0x46, 0x46};             // RIFF
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50};             // WEBP (offset 8)

    // ── 동영상 매직바이트 시그니처 ─────────────────────────
    // MP4/MOV: offset 4~7 = "ftyp"
    private static final byte[] FTYP_MARKER = {0x66, 0x74, 0x79, 0x70};
    // AVI: "RIFF" + offset 8 = "AVI "
    private static final byte[] AVI_MARKER  = {0x41, 0x56, 0x49, 0x20};
    // WebM: EBML 헤더
    private static final byte[] WEBM_MAGIC  = {0x1A, 0x45, (byte)0xDF, (byte)0xA3};

    private static final int SIGNATURE_READ_BYTES = 16;

    private FileValidator() {}

    /**
     * 이미지 파일 검증
     * - Content-Type 확인 (1차)
     * - 매직바이트 확인 (2차, 실제 파일 내용 기반)
     * - 파일 크기 확인
     */
    public static void validateImage(MultipartFile file) {
        validateNotEmpty(file);
        validateContentType(file, "image/");
        validateSize(file, MAX_IMAGE_SIZE, "이미지");
        validateImageSignature(readSignatureBytes(file));
    }

    /**
     * 동영상 파일 검증
     */
    public static void validateVideo(MultipartFile file) {
        validateNotEmpty(file);
        validateContentType(file, "video/");
        validateSize(file, MAX_VIDEO_SIZE, "동영상");
        validateVideoSignature(readSignatureBytes(file));
    }

    /**
     * 이미지 또는 동영상 자동 판별 검증
     */
    public static void validateImageOrVideo(MultipartFile file) {
        validateNotEmpty(file);
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (contentType.startsWith("image/")) {
            validateImage(file);
        } else if (contentType.startsWith("video/")) {
            validateVideo(file);
        } else {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    // ── Private helpers ───────────────────────────────────

    private static void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일이 비어있습니다.");
        }
    }

    private static void validateContentType(MultipartFile file, String expectedPrefix) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith(expectedPrefix)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private static void validateSize(MultipartFile file, long maxSize, String typeName) {
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    typeName + " 파일은 최대 " + (maxSize / 1024 / 1024) + "MB까지 허용됩니다.");
        }
    }

    private static byte[] readSignatureBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] bytes = new byte[SIGNATURE_READ_BYTES];
            int read = is.read(bytes);
            if (read < 4) {
                throw new BusinessException(ErrorCode.INVALID_FILE_SIGNATURE);
            }
            return bytes;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE_SIGNATURE);
        }
    }

    private static void validateImageSignature(byte[] sig) {
        if (startsWith(sig, JPEG_MAGIC))   return; // JPEG
        if (startsWith(sig, PNG_MAGIC))    return; // PNG
        if (startsWith(sig, GIF87_MAGIC))  return; // GIF87a
        if (startsWith(sig, GIF89_MAGIC))  return; // GIF89a
        // WebP: RIFF....WEBP
        if (startsWith(sig, WEBP_RIFF) && sig.length >= 12 &&
            sig[8] == WEBP_MARKER[0] && sig[9] == WEBP_MARKER[1] &&
            sig[10] == WEBP_MARKER[2] && sig[11] == WEBP_MARKER[3]) return;
        // HEIC/HEIF: offset 4 = "ftyp"
        if (sig.length >= 8 && sig[4] == FTYP_MARKER[0] && sig[5] == FTYP_MARKER[1] &&
            sig[6] == FTYP_MARKER[2] && sig[7] == FTYP_MARKER[3]) return;

        throw new BusinessException(ErrorCode.INVALID_FILE_SIGNATURE);
    }

    private static void validateVideoSignature(byte[] sig) {
        // MP4/MOV: offset 4~7 = "ftyp"
        if (sig.length >= 8 &&
            sig[4] == FTYP_MARKER[0] && sig[5] == FTYP_MARKER[1] &&
            sig[6] == FTYP_MARKER[2] && sig[7] == FTYP_MARKER[3]) return;
        // AVI: RIFF....AVI
        if (startsWith(sig, WEBP_RIFF) && sig.length >= 12 &&
            sig[8] == AVI_MARKER[0] && sig[9] == AVI_MARKER[1] &&
            sig[10] == AVI_MARKER[2] && sig[11] == AVI_MARKER[3]) return;
        // WebM
        if (startsWith(sig, WEBM_MAGIC)) return;

        throw new BusinessException(ErrorCode.INVALID_FILE_SIGNATURE);
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        return Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
    }
}
