package org.hunau.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.common.exception.BusinessException;
import org.hunau.common.util.AssertUtil;
import org.hunau.trace.client.TraceExternalClient;
import org.hunau.trace.entity.Company;
import org.hunau.trace.mapper.CompanyMapper;
import org.hunau.trace.model.req.InitCompanyRequest;
import org.hunau.trace.model.req.UpdateCompanyGovernanceRequest;
import org.hunau.trace.security.AuthPrincipal;
import org.hunau.trace.service.CompanyService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyServiceImpl implements CompanyService {

    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private TraceExternalClient traceExternalClient;

    @Override
    public R<?> initCompany(InitCompanyRequest request) {
        AssertUtil.notNull(request, "企业初始化信息不能为空");
        AssertUtil.notEmpty(request.getCompanyId(), "企业ID不能为空");
        AssertUtil.notEmpty(request.getName(), "企业名称不能为空");

        Company existed = companyMapper.selectById(request.getCompanyId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyId", request.getCompanyId());
        if (existed != null) {
            result.put("created", false);
            result.put("alreadyExists", true);
            result.put("company", existed);
            return R.ok(result);
        }

        Company company = new Company();
        company.setCompanyId(request.getCompanyId());
        company.setName(request.getName());
        company.setLevel(request.getLevel() == null || request.getLevel().isBlank() ? "一级" : request.getLevel());
        company.setAddress(request.getAddress() == null || request.getAddress().isBlank() ? "待完善" : request.getAddress());
        company.setContactPhone(request.getContactPhone() == null || request.getContactPhone().isBlank() ? "待完善" : request.getContactPhone());
        company.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        companyMapper.insert(company);
        result.put("created", true);
        result.put("alreadyExists", false);
        result.put("company", company);
        return R.ok(result);
    }

    @Override
    public R<?> createCompany(Company company) {
        AssertUtil.notNull(company, "企业信息不能为空");
        AssertUtil.notEmpty(company.getName(), "企业名称不能为空");
        AssertUtil.notEmpty(company.getAddress(), "企业地址不能为空");
        AssertUtil.notEmpty(company.getContactPhone(), "联系电话不能为空");
        String targetCompanyId = resolveTargetCompanyId(company.getCompanyId(), "创建企业建档");
        company.setCompanyId(targetCompanyId);

        if (!traceExternalClient.isCompanyApproved(company.getCompanyId())) {
            throw new BusinessException("企业未通过认证审核，禁止建档");
        }

        Company idExisted = companyMapper.selectById(company.getCompanyId());
        if (idExisted != null) {
            throw new BusinessException("该企业ID已建档，请使用更新接口");
        }

        Long existed = companyMapper.selectCount(new LambdaQueryWrapper<Company>()
                .eq(Company::getName, company.getName()));
        if (existed != null && existed > 0) {
            throw new BusinessException("企业名称已存在");
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
        AssertUtil.notEmpty(company.getName(), "企业名称不能为空");
        AssertUtil.notEmpty(company.getAddress(), "企业地址不能为空");
        AssertUtil.notEmpty(company.getContactPhone(), "联系电话不能为空");
        String targetCompanyId = resolveTargetCompanyId(company.getCompanyId(), "修改企业信息");
        company.setCompanyId(targetCompanyId);

        if (!traceExternalClient.isCompanyApproved(company.getCompanyId())) {
            throw new BusinessException("企业未通过认证审核，禁止修改建档信息");
        }

        Company existed = companyMapper.selectById(company.getCompanyId());
        if (existed == null) {
            throw new BusinessException("企业不存在");
        }

        // COMPANY can maintain profile fields, but cannot change governance fields.
        if (!isCurrentUserAdmin()) {
            company.setLevel(existed.getLevel());
            company.setStatus(existed.getStatus());
        }

        companyMapper.updateById(company);
        return R.ok(company);
    }

    @Override
    public R<?> updateGovernance(UpdateCompanyGovernanceRequest request) {
        AssertUtil.notNull(request, "治理参数不能为空");
        AssertUtil.notEmpty(request.getCompanyId(), "企业ID不能为空");

        Company existed = companyMapper.selectById(request.getCompanyId());
        if (existed == null) {
            throw new BusinessException("企业不存在");
        }

        boolean changed = false;
        if (request.getLevel() != null && !request.getLevel().isBlank()) {
            existed.setLevel(request.getLevel());
            changed = true;
        }
        if (request.getStatus() != null) {
            existed.setStatus(request.getStatus());
            changed = true;
        }
        if (!changed) {
            throw new BusinessException("至少需要提供 level 或 status 其中一个字段");
        }

        companyMapper.updateById(existed);
        return R.ok(existed);
    }

    @Override
    public R<?> removeById(String  companyId) {
        AssertUtil.notEmpty(companyId, "企业ID不能为空");
        Company existed = companyMapper.selectById(companyId);
        if (existed == null) {
            throw new BusinessException("企业不存在");
        }
        companyMapper.deleteById(companyId);
        return R.ok(companyId);
    }

    @Override
    public R<?> getCompanyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("未登录或登录状态已失效");
        }

        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof AuthPrincipal principal)) {
            throw new BusinessException("用户身份上下文异常，请重新登录");
        }

        if ("COMPANY".equalsIgnoreCase(principal.getRole())) {
            if (principal.getCompanyId() == null || principal.getCompanyId().isBlank()) {
                throw new BusinessException("企业账号缺少 companyId，请重新登录");
            }
            Company company = companyMapper.selectById(principal.getCompanyId());
            if (company == null) {
                throw new BusinessException("企业不存在");
            }
            return R.ok(company);
        } else if ("ADMIN".equalsIgnoreCase(principal.getRole())) {
            // 管理员可以返回所有企业信息
            List<Company> companies = companyMapper.selectList(new LambdaQueryWrapper<Company>()
                    .orderByDesc(Company::getCreatedAt));
            return R.ok(companies);
        } else {
            throw new BusinessException("权限不足，无法访问企业信息");
        }
    }

    @Override
    public R<?> updateCompanyInfo(Company company) {
        AssertUtil.notNull(company, "企业信息不能为空");
        AssertUtil.notEmpty(company.getName(), "企业名称不能为空");
        AssertUtil.notEmpty(company.getAddress(), "企业地址不能为空");
        AssertUtil.notEmpty(company.getContactPhone(), "联系电话不能为空");
        
        String targetCompanyId = resolveTargetCompanyId(company.getCompanyId(), "修改企业信息");
        company.setCompanyId(targetCompanyId);

        if (!traceExternalClient.isCompanyApproved(company.getCompanyId())) {
            throw new BusinessException("企业未通过认证审核，禁止修改建档信息");
        }

        Company existed = companyMapper.selectById(company.getCompanyId());
        if (existed == null) {
            throw new BusinessException("企业不存在");
        }

        // COMPANY can maintain profile fields, but cannot change governance fields.
        if (!isCurrentUserAdmin()) {
            company.setLevel(existed.getLevel());
            company.setStatus(existed.getStatus());
        }

        companyMapper.updateById(company);
        return R.ok(company);
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private String resolveTargetCompanyId(String requestCompanyId, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException("未登录或登录状态已失效");
        }

        String normalizedRequestCompanyId = requestCompanyId == null ? "" : requestCompanyId.trim();
        if (isCurrentUserAdmin()) {
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
            throw new BusinessException("企业账号只能操作本企业数据");
        }
        return tokenCompanyId;
    }
}
