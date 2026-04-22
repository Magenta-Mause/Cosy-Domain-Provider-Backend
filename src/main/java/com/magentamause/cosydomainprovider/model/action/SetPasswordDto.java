package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPasswordDto {
    @NotBlank
    @Size(min = 8)
    private String password;
}
