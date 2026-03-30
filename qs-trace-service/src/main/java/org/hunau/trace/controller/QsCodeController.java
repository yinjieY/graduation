package org.hunau.trace.controller;

import org.hunau.common.R;
import org.hunau.trace.entity.QsCode;
import org.hunau.trace.model.req.ChangeQsStatusRequest;
import org.hunau.trace.service.QsCodeService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/trace/qs")
public class QsCodeController {

    @Resource
    private QsCodeService qsCodeService;

    // 生成溯源二维码
    @PostMapping("/generate")
    public R<?> generate(@RequestBody QsCode qsCode) {
        return qsCodeService.generateQs(qsCode);
    }

    // 根据qsId查询溯源信息
    @GetMapping("/get/{qsId}")
    public R<?> get(@PathVariable String qsId) {
        return qsCodeService.getByQsId(qsId);
    }

    // 查询所有二维码
    @GetMapping("/list")
    public R<?> list() {
        return qsCodeService.listAll();
    }

    // 二维码生命周期管理（active/invalid/frozen/cancelled）
    @PutMapping("/{qsId}/status")
    public R<?> changeStatus(@PathVariable String qsId, @RequestBody ChangeQsStatusRequest request) {
        return qsCodeService.changeStatus(qsId, request.getStatus());
    }
}
