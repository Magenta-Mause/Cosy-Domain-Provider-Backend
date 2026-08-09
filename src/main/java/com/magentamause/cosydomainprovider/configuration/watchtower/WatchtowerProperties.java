package com.magentamause.cosydomainprovider.configuration.watchtower;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "watchtower")
public class WatchtowerProperties {

    private final Screenshots screenshots = new Screenshots();

    @Getter
    @Setter
    public static class Screenshots {
        /**
         * S3 endpoint of the in-cluster MinIO. Blank disables screenshot links entirely — the rest
         * of Watchtower keeps working, scans just render without an image. That is the default so
         * local dev needs no MinIO.
         */
        private String endpoint = "";

        private String region = "us-east-1";
        private String bucket = "cosy-watchtower-screenshots";
        private String accessKey = "";
        private String secretKey = "";

        /** How long a presigned screenshot URL stays valid. */
        private long presignTtlSeconds = 900;
    }
}
