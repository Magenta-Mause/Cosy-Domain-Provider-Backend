package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubdomainCreationDto {
    @NotBlank
    @Size(min = 1, max = 63)
    @Pattern(
            regexp = "^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$",
            message =
                    "Label must be 1-63 lowercase letters/digits/hyphens, not starting or ending with a hyphen")
    private String label;

    @NotBlank
    @Pattern(
            regexp =
                    "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
            message = "targetIp must be a valid IPv4 address")
    private String targetIp;
}
