package org.hunau.auth.model;

import lombok.Data;

@Data
public class CompanyApplyRequest {
    // Optional for compatibility/admin usage; COMPANY users should not provide it.
    private String companyId;
    private String companyName;
    private String remark;
}

