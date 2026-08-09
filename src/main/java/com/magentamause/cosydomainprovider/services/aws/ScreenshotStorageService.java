package com.magentamause.cosydomainprovider.services.aws;

import com.magentamause.cosydomainprovider.configuration.watchtower.WatchtowerProperties;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Turns MinIO object keys into short-lived presigned URLs the admin dashboard can put in an {@code
 * <img src>}. The bucket itself stays private — screenshots can show whatever a user hosted, so
 * they must never be anonymously readable the way the systemtest report bucket is.
 */
@Slf4j
@Service
public class ScreenshotStorageService {

    private final WatchtowerProperties.Screenshots config;
    private final S3Presigner presigner;

    public ScreenshotStorageService(WatchtowerProperties properties) {
        this.config = properties.getScreenshots();
        this.presigner = buildPresigner(config);
        if (presigner == null) {
            log.info(
                    "Watchtower screenshot storage disabled (no endpoint configured) — scans will"
                            + " render without images.");
        }
    }

    private static S3Presigner buildPresigner(WatchtowerProperties.Screenshots config) {
        if (config.getEndpoint() == null || config.getEndpoint().isBlank()) {
            return null;
        }
        return S3Presigner.builder()
                .endpointOverride(URI.create(config.getEndpoint()))
                .region(Region.of(config.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        config.getAccessKey(), config.getSecretKey())))
                // MinIO serves buckets as a path segment, not as a DNS subdomain.
                .serviceConfiguration(
                        software.amazon.awssdk.services.s3.S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build())
                .build();
    }

    /**
     * @return a presigned GET URL, or null when storage is disabled or the key is absent. Callers
     *     treat null as "no screenshot" rather than as an error.
     */
    public String presignedUrl(String objectKey) {
        if (presigner == null || objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            GetObjectPresignRequest request =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofSeconds(config.getPresignTtlSeconds()))
                            .getObjectRequest(
                                    GetObjectRequest.builder()
                                            .bucket(config.getBucket())
                                            .key(objectKey)
                                            .build())
                            .build();
            return presigner.presignGetObject(request).url().toString();
        } catch (RuntimeException e) {
            log.warn("Failed to presign screenshot {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    @PreDestroy
    public void close() {
        if (presigner != null) {
            presigner.close();
        }
    }
}
