package org.hunau.scan.client;

import org.hunau.common.model.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TraceFeignClientFallbackFactory implements FallbackFactory<TraceFeignClient> {

    @Override
    public TraceFeignClient create(Throwable cause) {
        return new TraceFeignClient() {
            @Override
            public R<Map<String, Object>> queryTrace(String qsId) {
                return R.fail("溯源服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }

            @Override
            public R<?> changeStatus(String qsId, Map<String, Object> body) {
                return R.fail("溯源服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }
        };
    }
}
