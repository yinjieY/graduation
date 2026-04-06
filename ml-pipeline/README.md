# Offline Training Pipeline (Task-Book Alignment)

This folder implements the required offline flow:

1. SQL-generated sample scan data
2. Feature extraction (`reuse_pattern` export)
3. Sequence/graph style dual-channel model training (Transformer + graph branch)
4. Knowledge distillation to a lightweight student model
5. ONNX export for Java `onnxruntime` inference

## 1) Install dependencies

```bash
pip install -r requirements.txt
```

## 2) Generate or refresh training data

Run SQL in MySQL:

- `sql/generate_training_data.sql`
- then call: `CALL gen_scan_training_data(50000);`

## 3) Refresh reuse features in service layer

Call scan-service aggregation endpoint:

```bash
curl -X POST http://localhost:9093/scan/reuse/refresh
```

## 4) Export `reuse_pattern` as CSV

```bash
python export_reuse_pattern.py --host 127.0.0.1 --port 3306 --user root --password 123456 --database yx_ai_feature --output data/reuse_pattern.csv
```

## 5) Train + distill + export ONNX

```bash
python train_pipeline.py --reuse-csv data/reuse_pattern.csv --teacher-epochs 30 --student-epochs 20 --onnx-out artifacts/qs_risk_model.onnx
```

## 6) Integrate with Java service

Copy the model to:

- `qs-alert-service/src/main/resources/qs_risk_model.onnx`

`AiRiskService` already loads `features -> logits` from ONNX Runtime.

