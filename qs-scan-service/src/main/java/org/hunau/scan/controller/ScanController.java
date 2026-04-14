package org.hunau.scan.controller;

import org.hunau.common.model.R;
import org.hunau.scan.model.ScanRequest;
import org.hunau.scan.service.ReusePatternScheduler;
import org.hunau.scan.service.ScanLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/scan")
public class ScanController {

    private final ScanLogService scanLogService;
    private final ReusePatternScheduler reusePatternScheduler;

    public ScanController(ScanLogService scanLogService, ReusePatternScheduler reusePatternScheduler) {
        this.scanLogService = scanLogService;
        this.reusePatternScheduler = reusePatternScheduler;
    }

    @PostMapping("/report")
    public R<?> report(@RequestBody ScanRequest request, HttpServletRequest httpServletRequest) {
        if (request.getIp() == null || request.getIp().isBlank()) {
            request.setIp(httpServletRequest.getRemoteAddr());
        }
        return scanLogService.handleScan(request);
    }

    @PostMapping("/reuse/refresh")
    public R<?> refreshReusePattern() {
        ReusePatternScheduler.RefreshSummary summary = reusePatternScheduler.refreshNowWithMeta();
        Map<String, Object> data = new HashMap<>();
        data.put("affectedRows", summary.affectedRows());
        data.put("snapshotTime", summary.snapshotTime());
        data.put("lookbackMinutes", summary.lookbackMinutes());
        data.put("minUpdateGapSeconds", summary.minUpdateGapSeconds());
        data.put("scanUpdateLagSeconds", summary.scanUpdateLagSeconds());
        data.put("message", "reuse_pattern聚合已触发");
        return R.ok(data);
    }

    @GetMapping("/reuse/{qsId}")
    public R<?> reuseFeature(@PathVariable String qsId) {
        return scanLogService.getReuseFeatureByQsId(qsId);
    }

    @GetMapping("/logs/{qsId}")
    public R<?> logs(@PathVariable String qsId) {
        return scanLogService.listByQsId(qsId);
    }
}
