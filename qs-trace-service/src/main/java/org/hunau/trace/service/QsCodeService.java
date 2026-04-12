package org.hunau.trace.service;

import org.hunau.common.R;
import org.hunau.trace.entity.QsCode;

public interface QsCodeService {
    R<?> generateQs(QsCode qsCode);

    R<?> getByQsId(String qsId);

    R<?> listAll();

    R<?> listByCompanyId(String companyId);

    R<?> changeStatus(String qsId, String status);

    R<?> changeStatusBySystem(String qsId, String status);
}
