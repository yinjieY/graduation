package org.hunau.auth.model;

import lombok.Data;

@Data
public class CompanyReviewRequest {
    private Boolean approved;
    private String companyName;
    private String remark;
}

