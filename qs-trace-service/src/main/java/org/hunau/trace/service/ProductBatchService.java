package org.hunau.trace.service;

import org.hunau.common.R;
import org.hunau.trace.entity.ProductBatch;

public interface ProductBatchService {
    R<?> createBatch(ProductBatch batch);

    R<?> listBatches(String companyId);

    R<?> updateBatch(ProductBatch batch);

    R<?> deleteBatch(String batchId);

    R<?> applyForReview(String batchId);

    R<?> reviewBatch(String batchId, String status, String comment);

    R<?> listPendingBatches();
}
