package org.hunau.auth.client;

import org.hunau.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "qs-alert-service",
        fallbackFactory = AlertFeignClientFallbackFactory.class
)
public interface AlertFeignClient {

    @PostMapping("/alert/admin/action/notify")
    R<Map<String, Object>> sendAdminActionNotification(@RequestBody Map<String, Object> body);
}