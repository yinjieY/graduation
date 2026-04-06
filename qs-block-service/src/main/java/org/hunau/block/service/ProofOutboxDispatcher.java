package org.hunau.block.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProofOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ProofOutboxDispatcher.class);

    private final ProofService proofService;

    public ProofOutboxDispatcher(ProofService proofService) {
        this.proofService = proofService;
    }

    @Scheduled(fixedDelayString = "${app.blockchain.outbox.dispatch-interval-ms:3000}",
            initialDelayString = "${app.blockchain.outbox.initial-delay-ms:5000}")
    public void dispatch() {
        int success = proofService.dispatchPendingOutbox();
        if (success > 0) {
            log.info("proof outbox dispatched success={}", success);
        }
    }
}

