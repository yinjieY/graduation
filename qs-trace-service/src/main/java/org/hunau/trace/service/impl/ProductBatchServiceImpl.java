package org.hunau.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.common.exception.BusinessException;
import org.hunau.common.util.AssertUtil;
import org.hunau.trace.entity.Company;
import org.hunau.trace.entity.ProductBatch;
import org.hunau.trace.mapper.CompanyMapper;
import org.hunau.trace.mapper.ProductBatchMapper;
import org.hunau.trace.service.ProductBatchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProductBatchServiceImpl implements ProductBatchService {

    @Resource
    private ProductBatchMapper productBatchMapper;
    @Resource
    private CompanyMapper companyMapper;

    @Override
    public R<?> createBatch(ProductBatch batch) {
        AssertUtil.notNull(batch, "批次信息不能为空");
        AssertUtil.notEmpty(batch.getCompanyId(), "企业ID不能为空");
        AssertUtil.notNull(batch.getProductionDate(), "生产日期不能为空");
        AssertUtil.notEmpty(batch.getIngredients(), "配料不能为空");
        AssertUtil.notEmpty(batch.getProductionStandard(), "生产标准不能为空");
        AssertUtil.notNull(batch.getTotalQuantity(), "总数量不能为空");

        Company company = companyMapper.selectById(batch.getCompanyId());
        if (company == null) {
            throw new BusinessException("企业不存在");
        }
        if (!Integer.valueOf(1).equals(company.getStatus())) {
            throw new BusinessException("企业状态不可用，无法创建批次");
        }

        if (batch.getBatchId() == null || batch.getBatchId().isBlank()) {
            batch.setBatchId(UUID.randomUUID().toString().replace("-", ""));
        }
        productBatchMapper.insert(batch);
        return R.ok(batch);
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
