package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminSettingsUpdateDto {
    @NotNull private Boolean domainCreationEnabled;
}
