package org.hunau.auth.model;

import lombok.Data;

@Data
public class CompanyApplyRequest {
    private String companyId;
    private String companyName;
    private String address;
    private String contactPhone;
    private Double lat;
    private Double lng;
    private String remark;
}

