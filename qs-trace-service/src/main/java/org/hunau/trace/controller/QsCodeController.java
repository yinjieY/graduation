package org.hunau.trace.controller;

import org.hunau.common.model.R;
import org.hunau.trace.entity.QsCode;
import org.hunau.trace.model.req.ChangeQsStatusRequest;
import org.hunau.trace.service.QrCodeImageService;
import org.hunau.trace.service.QsCodeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/trace/qs")
public class QsCodeController {

    @Resource
    private QsCodeService qsCodeService;
    @Resource
    private QrCodeImageService qrCodeImageService;

    // 生成溯源二维码
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> generate(@RequestBody QsCode qsCode) {
        return qsCodeService.generateQs(qsCode);
    }

    @GetMapping(value = "/image/{fileName}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<org.springframework.core.io.Resource> image(@PathVariable String fileName) {
        org.springframework.core.io.Resource image = qrCodeImageService.loadImage(fileName);
        if (!image.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(image);
    }

    // 根据qsId查询溯源信息
    @GetMapping("/get/{qsId}")
    public R<?> get(@PathVariable String qsId) {
        return qsCodeService.getByQsId(qsId);
    }

    // 查询所有二维码
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) String companyId) {
        return qsCodeService.listByCompanyId(companyId);
    }

    // 二维码生命周期管理（active/invalid/frozen/cancelled）
    @PutMapping("/{qsId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY')")
    public R<?> changeStatus(@PathVariable String qsId, @RequestBody ChangeQsStatusRequest request) {
        return qsCodeService.changeStatus(qsId, request.getStatus());
    }

    // 系统自动处置（例如预警服务触发冻结）- 仅管理员可访问
    @PutMapping("/{qsId}/status/internal")
    @PreAuthorize("hasRole('ADMIN')")
    public R<?> changeStatusInternal(@PathVariable String qsId, @RequestBody ChangeQsStatusRequest request) {
        return qsCodeService.changeStatus(qsId, request.getStatus());
    }

    // 系统自动处置（无需认证）- 用于预警服务等内部系统调用
    @PutMapping("/{qsId}/status/system")
    public R<?> changeStatusBySystem(@PathVariable String qsId, @RequestBody ChangeQsStatusRequest request) {
        return qsCodeService.changeStatusBySystem(qsId, request.getStatus());
    }
}
