package org.hunau.trace.service;

import org.hunau.common.R;
import org.hunau.trace.entity.Company;

public interface CompanyService {
    R<?> createCompany(Company company);

    R<?> listCompanies();

    R<?> updateById(Company company);

    R<?> removeById(String companyId);
}
