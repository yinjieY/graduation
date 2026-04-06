package org.hunau.alert.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class TraceFeignClientFallbackFactory implements FallbackFactory<TraceFeignClient> {

    @Override
    public TraceFeignClient create(Throwable cause) {
        return (qsId, body) -> R.fail("溯源服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
    }
}


