package org.hunau.trace.model.req;

import lombok.Data;

@Data
public class UpdateCompanyGovernanceRequest {
    private String companyId;
    private String level;
    private Integer status;
}

