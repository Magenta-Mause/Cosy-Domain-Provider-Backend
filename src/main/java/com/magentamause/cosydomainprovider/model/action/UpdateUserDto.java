package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDto {

    @Size(min = 3, max = 20)
    private String newUsername;

    private String currentPassword;

    @Size(min = 8)
    private String newPassword;
}
