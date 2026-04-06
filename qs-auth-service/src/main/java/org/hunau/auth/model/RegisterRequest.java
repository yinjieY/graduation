package org.hunau.auth.model;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String phone;
    private String password;
    private String role;
    // Legacy input kept for compatibility; backend now generates companyId.
    private String companyId;
    private String companyName;
    private String remark;
}
