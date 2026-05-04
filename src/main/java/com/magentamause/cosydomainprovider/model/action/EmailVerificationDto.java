package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerificationDto {
    @NotBlank private String token;
}
