package org.hunau.trace.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BlockFeignClientFallbackFactory implements FallbackFactory<BlockFeignClient> {

    @Override
    public BlockFeignClient create(Throwable cause) {
        return new BlockFeignClient() {
            @Override
            public R<?> saveQrProof(Map<String, Object> body) {
                return R.fail("区块链服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }

            @Override
            public R<?> saveFreezeProof(Map<String, Object> body) {
                return R.fail("区块链服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }
        };
    }
}

