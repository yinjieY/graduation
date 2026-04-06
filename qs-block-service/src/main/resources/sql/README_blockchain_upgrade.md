# Blockchain Upgrade Notes

This module now supports an outbox-based chain write workflow for proof records.

## What changed

- `blockchain_proof` now stores chain metadata: `chain_status`, `tx_hash`, `block_number`, `contract_address`, `chain_error`, `retry_count`, `idempotency_key`, `updated_at`.
- Added retry governance tables:
  - `proof_outbox`
  - `proof_dead_letter`
- Added scheduled dispatcher in `ProofOutboxDispatcher`.

## Apply schema

For fresh init, execute:
- `qs-block-service/src/main/resources/sql/init_block_schema.sql`

For existing environments, execute:
- `sql/patch_blockchain_outbox_upgrade.sql`

## Runtime behavior

1. `POST /block/proof/*` writes to `blockchain_proof` with `PENDING`.
2. Outbox row is created in `proof_outbox`.
3. Service tries one immediate chain write.
4. Scheduled dispatcher retries failed writes until max retries.
5. Exhausted retries are moved to dead-letter.

## Config

See `qs-block-service/src/main/resources/application.yml`:

- `app.blockchain.contract-address`
- `app.blockchain.outbox.dispatch-interval-ms`
- `app.blockchain.outbox.dispatch-batch-size`
- `app.blockchain.outbox.max-retries`
- `app.blockchain.outbox.retry-backoff-seconds`

