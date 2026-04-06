package org.hunau.trace.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(
        name = "qs-auth-service",
        url = "${app.clients.auth-service-url:}",
        fallbackFactory = AuthFeignClientFallbackFactory.class
)
public interface AuthFeignClient {

    @GetMapping("/auth/company/status/{companyId}")
    R<Map<String, Object>> queryCompanyStatus(@PathVariable("companyId") String companyId);
}



