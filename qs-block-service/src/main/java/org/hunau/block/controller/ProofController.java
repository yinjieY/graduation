package org.hunau.block.controller;

import org.hunau.block.model.ProofRecord;
import org.hunau.block.service.ProofService;
import org.hunau.common.R;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/block/proof")
public class ProofController {

    private final ProofService proofService;

    public ProofController(ProofService proofService) {
        this.proofService = proofService;
    }

    @PostMapping("/qr")
    public R<ProofRecord> saveQrProof(@RequestBody Map<String, Object> body) {
        String qsId = String.valueOf(body.get("qsId"));
        ProofRecord record = proofService.save(qsId, "QR_HASH", body.toString());
        return R.ok(record);
    }

    @PostMapping("/event")
    public R<ProofRecord> saveEventProof(@RequestBody Map<String, Object> body) {
        String eventId = String.valueOf(body.get("eventId"));
        ProofRecord record = proofService.save(eventId, "ALERT_EVENT", body.toString());
        return R.ok(record);
    }

    @PostMapping("/freeze")
    public R<ProofRecord> saveFreezeProof(@RequestBody Map<String, Object> body) {
        String qsId = String.valueOf(body.get("qsId"));
        ProofRecord record = proofService.save(qsId, "QR_FREEZE", body.toString());
        return R.ok(record);
    }

    @GetMapping("/verify")
    public R<Map<String, Object>> verify(@RequestParam String businessKey,
                                         @RequestParam String hash) {
        boolean verified = proofService.verify(businessKey, hash);
        Map<String, Object> data = new HashMap<>();
        data.put("businessKey", businessKey);
        data.put("verified", verified);
        return R.ok(data);
    }

    @GetMapping("/list/{businessKey}")
    public R<List<ProofRecord>> list(@PathVariable String businessKey) {
        return R.ok(proofService.list(businessKey));
    }
}

