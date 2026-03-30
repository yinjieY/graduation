package org.hunau.trace.model.vo;

import lombok.Data;
import org.hunau.trace.entity.Company;
import org.hunau.trace.entity.ProductBatch;
import org.hunau.trace.entity.QsCode;

@Data
public class TraceDetailVO {
    private QsCode qsCode;
    private ProductBatch batch;
    private Company company;
}
