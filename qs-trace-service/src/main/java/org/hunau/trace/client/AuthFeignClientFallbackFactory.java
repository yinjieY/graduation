package org.hunau.trace.client;

import org.hunau.common.model.R;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthFeignClientFallbackFactory implements FallbackFactory<AuthFeignClient> {

    @Override
    public AuthFeignClient create(Throwable cause) {
        return new AuthFeignClient() {
            @Override
            public R<Map<String, Object>> queryCompanyStatus(String companyId) {
                return R.fail("认证服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }

            @Override
            public R<Map<String, Object>> updateCompanyInfo(Map<String, String> request) {
                return R.fail("认证服务不可用: " + (cause == null ? "unknown" : cause.getMessage()));
            }
        };
    }
}
