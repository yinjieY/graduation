package org.hunau.alert.model;

public class FeedbackMessageRequest {

    private String feedbackId;
    private String qsId;
    private String companyId;
    private String feedbackType;
    private Double complaintRate;
    private String riskLevel;
    private Boolean forceNotify;

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getQsId() {
        return qsId;
    }

    public void setQsId(String qsId) {
        this.qsId = qsId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public Double getComplaintRate() {
        return complaintRate;
    }

    public void setComplaintRate(Double complaintRate) {
        this.complaintRate = complaintRate;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getForceNotify() {
        return forceNotify;
    }

    public void setForceNotify(Boolean forceNotify) {
        this.forceNotify = forceNotify;
    }
}
