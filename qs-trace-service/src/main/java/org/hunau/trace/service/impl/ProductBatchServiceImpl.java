package org.hunau.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.common.exception.BusinessException;
import org.hunau.common.util.AssertUtil;
import org.hunau.common.util.IdFormatUtil;
import org.hunau.trace.entity.Company;
import org.hunau.trace.entity.ProductBatch;
import org.hunau.trace.mapper.CompanyMapper;
import org.hunau.trace.mapper.ProductBatchMapper;
import org.hunau.trace.security.AuthPrincipal;
import org.hunau.trace.service.ProductBatchService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductBatchServiceImpl implements ProductBatchService {

    @Resource
    private ProductBatchMapper productBatchMapper;
    @Resource
    private CompanyMapper companyMapper;

    @Override
    public R<?> createBatch(ProductBatch batch) {
        AssertUtil.notNull(batch, "批次信息不能为空");
        AssertUtil.notNull(batch.getProductionDate(), "生产日期不能为空");
        AssertUtil.notEmpty(batch.getIngredients(), "配料不能为空");
        AssertUtil.notEmpty(batch.getProductionStandard(), "生产标准不能为空");
        AssertUtil.notNull(batch.getTotalQuantity(), "总数量不能为空");
        if (batch.getTotalQuantity() <= 0) {
            throw new BusinessException("总数量必须大于0");
        }

        String effectiveCompanyId = resolveTargetCompanyId(batch.getCompanyId(), "创建生产批次");
        batch.setCompanyId(effectiveCompanyId);

        Company company = companyMapper.selectById(effectiveCompanyId);
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        if (!Integer.valueOf(1).equals(company.getStatus())) {
            throw new BusinessException("企业状态不可用，无法创建批次");
        }

        if (batch.getBatchId() == null || batch.getBatchId().isBlank()) {
            batch.setBatchId(generateBatchId());
        }
        productBatchMapper.insert(batch);
        return R.ok(batch);
    }

    private String generateBatchId() {
        for (int i = 0; i < 8; i++) {
            String candidate = IdFormatUtil.newBatchId();
            if (productBatchMapper.selectById(candidate) == null) {
                return candidate;
            }
        }
        throw new BusinessException("批次ID生成失败，请重试");
    }

    private String resolveTargetCompanyId(String requestCompanyId, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("未登录或登录状态已失效");
        }
        String normalizedRequestCompanyId = requestCompanyId == null ? "" : requestCompanyId.trim();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            if (normalizedRequestCompanyId.isEmpty()) {
                throw new BusinessException("管理员" + action + "必须传 companyId");
            }
            return normalizedRequestCompanyId;
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof AuthPrincipal principal)) {
            throw new BusinessException("用户身份上下文异常，请重新登录");
        }

        if (!"COMPANY".equalsIgnoreCase(principal.getRole())) {
            throw new BusinessException("仅管理员或企业账号可" + action);
        }
        if (principal.getCompanyId() == null || principal.getCompanyId().isBlank()) {
            throw new BusinessException("企业账号缺少 companyId，请重新登录");
        }
        String tokenCompanyId = principal.getCompanyId().trim();
        if (!normalizedRequestCompanyId.isEmpty() && !tokenCompanyId.equals(normalizedRequestCompanyId)) {
            throw new BusinessException("企业账号只能创建本企业的生产批次");
        }
        return tokenCompanyId;
    }

    @Override
    public R<?> listBatches(String companyId) {
        LambdaQueryWrapper<ProductBatch> wrapper = new LambdaQueryWrapper<ProductBatch>()
                .orderByDesc(ProductBatch::getCreatedAt);
        if (companyId != null && !companyId.isBlank()) {
            wrapper.eq(ProductBatch::getCompanyId, companyId);
        }
        List<ProductBatch> batches = productBatchMapper.selectList(wrapper);
        return R.ok(batches);
    }
}
