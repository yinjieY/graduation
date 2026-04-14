package org.hunau.trace.client;

import org.hunau.common.model.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AlertFeignClientFallbackFactory implements FallbackFactory<AlertFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(AlertFeignClientFallbackFactory.class);

    @Override
    public AlertFeignClient create(Throwable cause) {
        return new AlertFeignClient() {
            @Override
            public R<Map<String, Object>> sendAdminActionNotification(Map<String, Object> body) {
                log.warn("Alert service notification fallback triggered: {}", cause.getMessage());
                return R.ok(Map.of("notificationId", null, "created", false, "message", "通知服务暂时不可用"));
            }
        };
    }
}