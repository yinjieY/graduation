package org.hunau.trace.model.req;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateProductBatchRequest {
    private String companyId;
    private LocalDateTime productionDate;
    private String ingredients;
    private String productionStandard;
    private Integer totalQuantity;
}

