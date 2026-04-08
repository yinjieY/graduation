package org.hunau.alert.model;

import lombok.Data;

@Data
public class AlertDisposalCompleteRequest {
    private Long alertId;
    private String handleUser;
    private String handleNote;
}

