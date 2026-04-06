package org.hunau.trace.model.req;

import lombok.Data;

@Data
public class InitCompanyRequest {
    private String companyId;
    private String name;
    private String level;
    private String address;
    private String contactPhone;
    private Integer status;
}

