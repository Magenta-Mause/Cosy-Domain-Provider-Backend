package com.magentamause.cosydomainprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "global_settings")
@Getter
@Setter
@NoArgsConstructor
public class GlobalSettingsEntity {

    @Id private String id = "global";

    @Column(nullable = false)
    private boolean domainCreationEnabled = true;
}
