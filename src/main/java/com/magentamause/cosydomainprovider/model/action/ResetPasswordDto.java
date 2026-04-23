package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDto {
    @NotBlank private String token;

    @Size(min = 8)
    private String newPassword;
}
