package org.hunau.scan.controller;

import org.hunau.common.R;
import org.hunau.scan.model.ScanRequest;
import org.hunau.scan.service.ScanLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/scan")
public class ScanController {

    private final ScanLogService scanLogService;

    public ScanController(ScanLogService scanLogService) {
        this.scanLogService = scanLogService;
    }

    @PostMapping("/report")
    public R<?> report(@RequestBody ScanRequest request) {
        return scanLogService.handleScan(request);
    }

    @GetMapping("/logs/{qsId}")
    public R<?> logs(@PathVariable String qsId) {
        return scanLogService.listByQsId(qsId);
    }
}

