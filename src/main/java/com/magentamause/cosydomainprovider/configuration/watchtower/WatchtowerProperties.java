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
         * S3 endpoint used to build presigned screenshot URLs. This must be the endpoint the
         * <em>admin's browser</em> can reach, not the in-cluster Service address: a SigV4 signature
         * covers the host, so a URL signed for {@code minio.minio.svc.cluster.local} cannot be
         * rewritten client-side and simply fails to load. The backend never uploads and never calls
         * MinIO itself — presigning is local computation — so it has no reason to prefer the
         * internal address.
         *
         * <p>Blank disables screenshot links entirely; the rest of Watchtower keeps working and
         * scans render without an image. That is the default so local dev needs no MinIO.
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
