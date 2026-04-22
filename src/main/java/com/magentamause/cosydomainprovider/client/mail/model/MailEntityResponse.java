package com.magentamause.cosydomainprovider.client.mail.model;

import lombok.Data;

@Data
public class MailEntityResponse {
    private String id;
    private String recipient;
    private String subject;
    private String body;
    private String sentDate;
    private boolean success;
    private boolean enableHtml;
}
