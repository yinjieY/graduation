package org.hunau.scan.model;

public class FeedbackStatusUpdateRequest {

    private String status;
    private String handleNote;
    private Boolean freezeQrcode;
    private Boolean notifyCompany;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public Boolean getFreezeQrcode() {
        return freezeQrcode;
    }

    public void setFreezeQrcode(Boolean freezeQrcode) {
        this.freezeQrcode = freezeQrcode;
    }

    public Boolean getNotifyCompany() {
        return notifyCompany;
    }

    public void setNotifyCompany(Boolean notifyCompany) {
        this.notifyCompany = notifyCompany;
    }
}

