package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserUpdateDto {
    @Size(min = 3, max = 20)
    private String username;

    @Email private String email;
}
