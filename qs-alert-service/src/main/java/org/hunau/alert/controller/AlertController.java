package org.hunau.alert.controller;

import org.hunau.alert.model.AlertEvaluateRequest;
import org.hunau.alert.service.AlertService;
import org.hunau.common.R;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alert")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping("/evaluate")
    public R<?> evaluate(@RequestBody AlertEvaluateRequest request) {
        return alertService.evaluate(request);
    }

    @GetMapping("/list")
    public R<?> list() {
        return alertService.list();
    }
}

