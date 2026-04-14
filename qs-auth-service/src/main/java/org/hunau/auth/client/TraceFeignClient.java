package org.hunau.auth.client;

import org.hunau.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "qs-trace-service",
        url = "${app.clients.trace-service-url:}",
        fallbackFactory = TraceFeignClientFallbackFactory.class
)
public interface TraceFeignClient {

    @PostMapping("/trace/company/init")
    R<Map<String, Object>> initCompany(@RequestBody Map<String, Object> body);
}

