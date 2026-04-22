package com.magentamause.cosydomainprovider.client.mail.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMailDto {
    public String recipient;
    public String subject;
    public String body;
    public boolean enableHtml;
}
