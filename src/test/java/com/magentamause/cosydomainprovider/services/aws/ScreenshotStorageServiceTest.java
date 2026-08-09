package com.magentamause.cosydomainprovider.services.aws;

import static org.assertj.core.api.Assertions.*;

import com.magentamause.cosydomainprovider.configuration.watchtower.WatchtowerProperties;
import org.junit.jupiter.api.Test;

class ScreenshotStorageServiceTest {

    private static WatchtowerProperties properties(String endpoint) {
        WatchtowerProperties properties = new WatchtowerProperties();
        WatchtowerProperties.Screenshots screenshots = properties.getScreenshots();
        screenshots.setEndpoint(endpoint);
        screenshots.setRegion("us-east-1");
        screenshots.setBucket("cosy-watchtower-screenshots");
        screenshots.setAccessKey("access");
        screenshots.setSecretKey("secret");
        screenshots.setPresignTtlSeconds(900);
        return properties;
    }

    @Test
    void presignedUrl_storageDisabled_returnsNull() {
        ScreenshotStorageService service = new ScreenshotStorageService(properties(""));

        assertThat(service.presignedUrl("2026-08-09/swift-gecko.png")).isNull();
    }

    @Test
    void presignedUrl_storageEnabled_signsBucketAndKeyPathStyle() {
        ScreenshotStorageService service =
                new ScreenshotStorageService(
                        properties("http://minio.minio.svc.cluster.local:9000"));

        String url = service.presignedUrl("2026-08-09/swift-gecko.png");

        assertThat(url).isNotNull();
        // MinIO addresses buckets as a path segment; a virtual-host style URL would
        // resolve to a hostname that does not exist in the cluster.
        assertThat(url).contains("/cosy-watchtower-screenshots/2026-08-09/swift-gecko.png");
        assertThat(url).contains("X-Amz-Signature");
        assertThat(url).contains("X-Amz-Expires=900");

        service.close();
    }

    @Test
    void presignedUrl_nullKey_returnsNullWithoutSigning() {
        ScreenshotStorageService service =
                new ScreenshotStorageService(properties("http://minio:9000"));

        assertThat(service.presignedUrl(null)).isNull();

        service.close();
    }

    @Test
    void presignedUrl_blankKey_returnsNull() {
        ScreenshotStorageService service =
                new ScreenshotStorageService(properties("http://minio:9000"));

        assertThat(service.presignedUrl("   ")).isNull();

        service.close();
    }

    @Test
    void close_whenDisabled_doesNotThrow() {
        ScreenshotStorageService service = new ScreenshotStorageService(properties(""));

        assertThatCode(service::close).doesNotThrowAnyException();
    }
}
