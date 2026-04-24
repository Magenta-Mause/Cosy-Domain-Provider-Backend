package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubdomainCreationDto {
    private String label;

    @Pattern(
            regexp =
                    "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            message = "targetIp must be a valid IPv4 address")
    private String targetIp;

    @Pattern(
            regexp =
                    "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
                            + "|([0-9a-fA-F]{1,4}:){1,7}:"
                            + "|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}"
                            + "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}"
                            + "|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}"
                            + "|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}"
                            + "|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}"
                            + "|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})"
                            + "|:((:[0-9a-fA-F]{1,4}){1,7}|:)"
                            + "|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+"
                            + "|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])"
                            + "|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))$",
            message = "targetIpv6 must be a valid IPv6 address")
    private String targetIpv6;

    @AssertTrue(message = "At least one of targetIp or targetIpv6 must be provided")
    public boolean isAtLeastOneIpProvided() {
        boolean hasIpv4 = targetIp != null && !targetIp.isBlank();
        boolean hasIpv6 = targetIpv6 != null && !targetIpv6.isBlank();
        return hasIpv4 || hasIpv6;
    }
}
