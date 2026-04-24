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
public class AdminSubdomainRelabelDto {
    @NotBlank
    @Size(min = 3, max = 45)
    @Pattern(
            regexp = "^[a-z0-9-]+$",
            message = "label must only contain lowercase letters, digits, and hyphens")
    private String label;
}
