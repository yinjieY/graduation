package org.hunau.trace.service;

import org.hunau.common.R;
import org.hunau.trace.entity.Company;
import org.hunau.trace.model.req.InitCompanyRequest;
import org.hunau.trace.model.req.UpdateCompanyGovernanceRequest;

public interface CompanyService {
    R<?> initCompany(InitCompanyRequest request);

    R<?> createCompany(Company company);

    R<?> listCompanies();

    R<?> updateById(Company company);

    R<?> updateGovernance(UpdateCompanyGovernanceRequest request);

    R<?> removeById(String companyId);
    
    R<?> getCompanyInfo();
    
    R<?> updateCompanyInfo(Company company);
    
    R<?> getCompanyById(String companyId);
}
