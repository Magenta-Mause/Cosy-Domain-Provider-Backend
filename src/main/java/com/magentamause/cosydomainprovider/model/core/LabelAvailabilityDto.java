package com.magentamause.cosydomainprovider.model.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LabelAvailabilityDto {
    private boolean available;
    private String reason;

    public static LabelAvailabilityDto available() {
        return new LabelAvailabilityDto(true, null);
    }

    public static LabelAvailabilityDto unavailable(String reason) {
        return new LabelAvailabilityDto(false, reason);
    }
}
