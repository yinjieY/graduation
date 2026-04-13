package org.hunau.alert.client;

import org.hunau.common.R;
import org.hunau.alert.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "qs-trace-service",
        url = "${app.clients.trace-service-url:}",
        configuration = FeignClientConfig.class,
        fallbackFactory = TraceFeignClientFallbackFactory.class
)
public interface TraceFeignClient {

    @PutMapping("/trace/qs/{qsId}/status/system")
    R<?> changeStatus(@PathVariable("qsId") String qsId, @RequestBody Map<String, Object> body);

    @GetMapping("/trace/qs/get/{qsId}")
    R<?> getQsCodeDetail(@PathVariable("qsId") String qsId);
}


