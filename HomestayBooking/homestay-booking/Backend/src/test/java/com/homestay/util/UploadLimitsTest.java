package com.homestay.util;

import com.homestay.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadLimitsTest {

    @Test
    void validateNotEmpty_withValidFile_doesNotThrow() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        assertThatCode(() -> UploadLimits.validateNotEmpty(file, "File empty"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNotEmpty_withNullFile_throwsException() {
        assertThatThrownBy(() -> UploadLimits.validateNotEmpty(null, "File empty"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File empty");
    }

    @Test
    void validateNotEmpty_withEmptyFile_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> UploadLimits.validateNotEmpty(file, "File empty"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File empty");
    }

    @Test
    void validateSize_withFileUnderLimit_doesNotThrow() {
        byte[] content = new byte[1024 * 1024];
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertThatCode(() -> UploadLimits.validateSize(file, "Image"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateSize_withFileOverLimit_throwsException() {
        byte[] content = new byte[(int) (11L * 1024 * 1024)];
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertThatThrownBy(() -> UploadLimits.validateSize(file, "Image"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("10MB");
    }

    @Test
    void validateSize_withNullFile_doesNotThrow() {
        assertThatCode(() -> UploadLimits.validateSize(null, "Image"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRoomImageMime_withJpegFile_doesNotThrow() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        assertThatCode(() -> UploadLimits.validateRoomImageMime(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRoomImageMime_withPngFile_doesNotThrow() {
        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "content".getBytes());

        assertThatCode(() -> UploadLimits.validateRoomImageMime(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRoomImageMime_withWebpFile_doesNotThrow() {
        MultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", "content".getBytes());

        assertThatCode(() -> UploadLimits.validateRoomImageMime(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRoomImageMime_withJpgContentType_doesNotThrow() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpg", "content".getBytes());

        assertThatCode(() -> UploadLimits.validateRoomImageMime(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRoomImageMime_withInvalidContentType_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", "content".getBytes());

        assertThatThrownBy(() -> UploadLimits.validateRoomImageMime(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPEG");
    }

    @Test
    void validateRoomImageMime_withInvalidExtension_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "test.bmp", "image/jpeg", "content".getBytes());

        assertThatThrownBy(() -> UploadLimits.validateRoomImageMime(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPEG");
    }

    @Test
    void validateRoomImageMime_withNullFile_throwsException() {
        assertThatThrownBy(() -> UploadLimits.validateRoomImageMime(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trống");
    }

    @Test
    void validateRoomImageMime_withEmptyFile_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> UploadLimits.validateRoomImageMime(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trống");
    }

    @Test
    void validateRoomImageMime_withMixedCaseExtension_doesNotThrow() {
        MultipartFile file = new MockMultipartFile("file", "test.JPG", "image/jpeg", "content".getBytes());

        assertThatCode(() -> UploadLimits.validateRoomImageMime(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateMaintenanceImage_withValidFile_doesNotThrow() {
        byte[] content = new byte[1024 * 1024];
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertThatCode(() -> UploadLimits.validateMaintenanceImage(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateMaintenanceImage_withFileOver5MB_throwsException() {
        byte[] content = new byte[(int) (6L * 1024 * 1024)];
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertThatThrownBy(() -> UploadLimits.validateMaintenanceImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void validateMaintenanceImage_withInvalidMime_throwsException() {
        MultipartFile file = new MockMultipartFile("file", "test.gif", "image/gif", "content".getBytes());

        assertThatThrownBy(() -> UploadLimits.validateMaintenanceImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JPEG");
    }

    @Test
    void constants_haveExpectedValues() {
        assertThat(UploadLimits.MAX_BYTES).isEqualTo(10L * 1024 * 1024);
        assertThat(UploadLimits.MAINTENANCE_IMAGE_MAX_BYTES).isEqualTo(5L * 1024 * 1024);
        assertThat(UploadLimits.MAX_ROOM_IMAGES).isEqualTo(10);
    }
}
