package org.hunau.scan.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "qs-alert-service",
        url = "${app.clients.alert-service-url:}",
        fallbackFactory = AlertFeignClientFallbackFactory.class
)
public interface AlertFeignClient {

    @PostMapping("/alert/evaluate")
    R<?> evaluate(@RequestBody Map<String, Object> body);
}



