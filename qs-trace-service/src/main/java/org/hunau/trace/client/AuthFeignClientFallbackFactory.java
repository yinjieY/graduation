package org.hunau.trace.client;

import org.hunau.common.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthFeignClientFallbackFactory implements FallbackFactory<AuthFeignClient> {

    @Override
    public AuthFeignClient create(Throwable cause) {
        return companyId -> R.fail("认证服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
    }
}


