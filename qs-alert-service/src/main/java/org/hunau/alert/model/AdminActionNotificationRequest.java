package org.hunau.alert.model;

public class AdminActionNotificationRequest {

    private String companyId;
    private String companyName;
    private String actionType;
    private String actionContent;
    private String sourceModule;
    private String sourceId;
    private String operator;

    public AdminActionNotificationRequest() {
    }

    public AdminActionNotificationRequest(String companyId, String companyName, String actionType, 
                                          String actionContent, String sourceModule, String sourceId, String operator) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.actionType = actionType;
        this.actionContent = actionContent;
        this.sourceModule = sourceModule;
        this.sourceId = sourceId;
        this.operator = operator;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionContent() {
        return actionContent;
    }

    public void setActionContent(String actionContent) {
        this.actionContent = actionContent;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}