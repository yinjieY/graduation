package org.hunau.trace.controller;

import org.hunau.common.model.R;
import org.hunau.trace.service.QsCodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trace/query")
public class TraceQueryController {

    private final QsCodeService qsCodeService;

    public TraceQueryController(QsCodeService qsCodeService) {
        this.qsCodeService = qsCodeService;
    }

    @GetMapping("/{qsId}")
    public R<?> query(@PathVariable String qsId) {
        return qsCodeService.getByQsId(qsId);
    }
}

