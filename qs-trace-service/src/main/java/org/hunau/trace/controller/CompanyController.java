package org.hunau.trace.controller;

import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Update;
import org.hunau.common.R;
import org.hunau.trace.entity.Company;
import org.hunau.trace.service.CompanyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trace/company")
public class CompanyController {

    @Resource
    private CompanyService companyService;

    @PostMapping("/create")
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
    public String update(@RequestBody Company company) {
        companyService.updateById(company);
        return "修改成功";
    }

    /**
     * 删除企业信息
     */
    @DeleteMapping("/delete/{companyId}")
    public String delete(@PathVariable String companyId) {
        companyService.removeById(companyId);
        return "删除成功";
    }
}
