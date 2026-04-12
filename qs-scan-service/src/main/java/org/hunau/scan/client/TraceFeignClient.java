package org.hunau.scan.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "qs-trace-service",
        url = "${app.clients.trace-service-url:}",
        fallbackFactory = org.hunau.scan.client.TraceFeignClientFallbackFactory.class
)
public interface TraceFeignClient {

    @GetMapping("/trace/query/{qsId}")
    R<Map<String, Object>> queryTrace(@PathVariable("qsId") String qsId);

    @PutMapping("/trace/qs/{qsId}/status/system")
    R<?> changeStatus(@PathVariable("qsId") String qsId, @RequestBody Map<String, Object> body);
}
