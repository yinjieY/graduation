package org.hunau.trace.client;

import org.hunau.common.R;
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

    @PostMapping("/block/proof/qr")
    R<?> saveQrProof(@RequestBody Map<String, Object> body);

    @PostMapping("/block/proof/freeze")
    R<?> saveFreezeProof(@RequestBody Map<String, Object> body);
}



