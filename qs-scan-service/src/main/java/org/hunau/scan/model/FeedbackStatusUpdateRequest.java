package org.hunau.scan.model;

public class FeedbackStatusUpdateRequest {

    private String status;
    private String handleNote;

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
}

