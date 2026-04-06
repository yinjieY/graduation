package org.hunau.trace.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthPrincipal {
    private String username;
    private String role;
    private String companyId;
}

