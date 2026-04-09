package org.hunau.scan.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AlertFeignClientFallbackFactory implements FallbackFactory<AlertFeignClient> {

    @Override
    public AlertFeignClient create(Throwable cause) {
        return new AlertFeignClient() {
            @Override
            public R<?> evaluate(java.util.Map<String, Object> body) {
                return R.fail("预警服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }

            @Override
            public R<?> createFeedbackMessage(java.util.Map<String, Object> body) {
                return R.fail("预警服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }
        };
    }
}


