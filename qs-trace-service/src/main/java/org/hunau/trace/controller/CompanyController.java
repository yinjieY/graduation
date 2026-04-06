package org.hunau.trace.controller;

import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.trace.entity.Company;
import org.hunau.trace.model.req.InitCompanyRequest;
import org.hunau.trace.model.req.UpdateCompanyGovernanceRequest;
import org.hunau.trace.service.CompanyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trace/company")
public class CompanyController {

    @Resource
    private CompanyService companyService;

    @PostMapping("/init")
    public R<?> init(@RequestBody InitCompanyRequest request) {
        return companyService.initCompany(request);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> create(@RequestBody Company company) {
        return companyService.createCompany(company);
    }

    @GetMapping("/list")
    public R<?> list() {
        return companyService.listCompanies();
    }

    /**
     * 修改企业信息
     */
    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> update(@RequestBody Company company) {
        return companyService.updateById(company);
    }

    @PutMapping("/governance")
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> updateGovernance(@RequestBody UpdateCompanyGovernanceRequest request) {
        return companyService.updateGovernance(request);
    }

    /**
     * 删除企业信息
     */
    @DeleteMapping("/delete/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> delete(@PathVariable String companyId) {
        return companyService.removeById(companyId);
    }
}
