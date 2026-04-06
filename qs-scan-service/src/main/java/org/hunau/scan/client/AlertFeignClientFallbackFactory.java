package org.hunau.scan.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AlertFeignClientFallbackFactory implements FallbackFactory<AlertFeignClient> {

    @Override
    public AlertFeignClient create(Throwable cause) {
        return body -> R.fail("预警服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
    }
}


