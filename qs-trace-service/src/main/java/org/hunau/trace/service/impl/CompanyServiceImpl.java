package org.hunau.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.common.exception.BusinessException;
import org.hunau.common.util.AssertUtil;
import org.hunau.trace.entity.Company;
import org.hunau.trace.mapper.CompanyMapper;
import org.hunau.trace.service.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyServiceImpl implements CompanyService {

    @Resource
    private CompanyMapper companyMapper;

    @Override
    public R<?> createCompany(Company company) {
        AssertUtil.notNull(company, "企业信息不能为空");
        AssertUtil.notEmpty(company.getName(), "企业名称不能为空");
        AssertUtil.notEmpty(company.getAddress(), "企业地址不能为空");
        AssertUtil.notEmpty(company.getContactPhone(), "联系电话不能为空");

        Long existed = companyMapper.selectCount(new LambdaQueryWrapper<Company>()
                .eq(Company::getName, company.getName()));
        if (existed != null && existed > 0) {
            throw new BusinessException("企业名称已存在");
        }

        if (company.getCompanyId() == null || company.getCompanyId().isBlank()) {
            company.setCompanyId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (company.getStatus() == null) {
            company.setStatus(1);
        }
        if (company.getLevel() == null || company.getLevel().isBlank()) {
            company.setLevel("一级");
        }

        companyMapper.insert(company);
        return R.ok(company);
    }

    @Override
    public R<?> listCompanies() {
        List<Company> companies = companyMapper.selectList(new LambdaQueryWrapper<Company>()
                .orderByDesc(Company::getCreatedAt));
        return R.ok(companies);
    }

    @Override
    public R<?> updateById(Company company) {
        AssertUtil.notNull(company, "企业信息不能为空");
        AssertUtil.notEmpty(company.getCompanyId(), "企业ID不能为空");
        AssertUtil.notEmpty(company.getName(), "企业名称不能为空");
        AssertUtil.notEmpty(company.getAddress(), "企业地址不能为空");
        AssertUtil.notEmpty(company.getContactPhone(), "联系电话不能为空");

        Company existed = companyMapper.selectById(company.getCompanyId());
        if (existed == null) {
            throw new BusinessException("企业不存在");
        }
        companyMapper.updateById(company);
        return R.ok(company);
    }

    @Override
    public R<?> removeById(String  companyId) {
        companyMapper.deleteById(companyId);
        return R.ok(companyId);
    }
}
