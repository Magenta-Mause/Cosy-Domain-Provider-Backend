package com.magentamause.cosydomainprovider.client.mail.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMailDto {
    private String recipient;
    private String subject;
    private String body;
    private boolean enableHtml;
}
