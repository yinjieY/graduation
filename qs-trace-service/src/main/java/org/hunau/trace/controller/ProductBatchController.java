package org.hunau.trace.controller;

import jakarta.annotation.Resource;
import org.hunau.common.R;
import org.hunau.trace.entity.ProductBatch;
import org.hunau.trace.service.ProductBatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trace/batch")
public class ProductBatchController {

    @Resource
    private ProductBatchService productBatchService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> create(@RequestBody ProductBatch batch) {
        return productBatchService.createBatch(batch);
    }

    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) String companyId) {
        return productBatchService.listBatches(companyId);
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> update(@RequestBody ProductBatch batch) {
        return productBatchService.updateBatch(batch);
    }

    @DeleteMapping("/delete/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> delete(@PathVariable String batchId, @RequestParam(required = false) String companyId) {
        return productBatchService.deleteBatch(batchId);
    }

    @PostMapping("/{batchId}/apply-review")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> applyReview(@PathVariable String batchId) {
        return productBatchService.applyForReview(batchId);
    }

    @PutMapping("/{batchId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> review(@PathVariable String batchId, @RequestParam String status, @RequestParam(required = false) String comment) {
        return productBatchService.reviewBatch(batchId, status, comment);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> listPending() {
        return productBatchService.listPendingBatches();
    }
}
