package org.hunau.block.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hunau.block.model.ProofRecord;
import org.hunau.block.service.ProofService;
import org.hunau.common.R;
import org.hunau.common.exception.BusinessException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/block/proof")
public class ProofController {

    private final ProofService proofService;
    private final ObjectMapper objectMapper;

    public ProofController(ProofService proofService) {
        this.proofService = proofService;
        this.objectMapper = new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    @PostMapping("/qr")
    public R<ProofRecord> saveQrProof(@RequestBody Map<String, Object> body) {
        String qsId = requiredString(body.get("qsId"), "qsId不能为空");
        ProofRecord record = proofService.save(qsId, "QR_HASH", toStableJson(body));
        return R.ok(record);
    }

    @PostMapping("/event")
    public R<ProofRecord> saveEventProof(@RequestBody Map<String, Object> body) {
        String eventId = requiredString(body.get("eventId"), "eventId不能为空");
        ProofRecord record = proofService.save(eventId, "ALERT_EVENT", toStableJson(body));
        return R.ok(record);
    }

    @PostMapping("/freeze")
    public R<ProofRecord> saveFreezeProof(@RequestBody Map<String, Object> body) {
        String qsId = requiredString(body.get("qsId"), "qsId不能为空");
        ProofRecord record = proofService.save(qsId, "QR_FREEZE", toStableJson(body));
        return R.ok(record);
    }

    @GetMapping("/verify")
    public R<Map<String, Object>> verify(@RequestParam String businessKey,
                                         @RequestParam String hash) {
        Map<String, Object> data = proofService.verifyDetail(businessKey, hash);
        return R.ok(data);
    }

    @GetMapping("/list/{businessKey}")
    public R<List<ProofRecord>> list(@PathVariable String businessKey) {
        return R.ok(proofService.list(businessKey));
    }

    private String toStableJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("存证载荷序列化失败");
        }
    }

    private String requiredString(Object value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        String str = String.valueOf(value).trim();
        if (str.isEmpty() || "null".equalsIgnoreCase(str)) {
            throw new BusinessException(message);
        }
        return str;
    }
}
