package org.hunau.alert.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class BlockFeignClientFallbackFactory implements FallbackFactory<BlockFeignClient> {

    @Override
    public BlockFeignClient create(Throwable cause) {
        return body -> R.fail("区块链服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
    }
}


