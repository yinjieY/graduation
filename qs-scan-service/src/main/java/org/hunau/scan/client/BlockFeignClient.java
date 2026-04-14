package org.hunau.scan.client;

import org.hunau.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "qs-block-service",
        url = "${app.clients.block-service-url:}",
        fallbackFactory = BlockFeignClientFallbackFactory.class
)
public interface BlockFeignClient {

    @PostMapping("/block/proof/event")
    R<?> saveEventProof(@RequestBody Map<String, Object> body);
}

