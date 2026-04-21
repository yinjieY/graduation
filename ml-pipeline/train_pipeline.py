import argparse
from pathlib import Path

import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.nn.functional as F
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, roc_auc_score, confusion_matrix


FEATURE_COLUMNS = [
    "scan_count",
    "time_variance",
    "location_variance",
    "device_count",
    "ip_count",
]


class DualChannelRiskModel(nn.Module):
    def __init__(self, in_dim: int = 5, hidden_dim: int = 32):
        super().__init__()
        self.time_proj = nn.Linear(in_dim, hidden_dim)
        encoder_layer = nn.TransformerEncoderLayer(
            d_model=hidden_dim,
            nhead=4,
            dim_feedforward=hidden_dim * 2,
            batch_first=True,
            dropout=0.2,
        )
        self.transformer = nn.TransformerEncoder(encoder_layer, num_layers=2)

        self.graph_branch = nn.Sequential(
            nn.Linear(in_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
        )

        self.head = nn.Sequential(
            nn.Linear(hidden_dim * 2, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, 2),
        )

    def forward(self, x):
        seq = self.time_proj(x).unsqueeze(1)
        seq_out = self.transformer(seq).squeeze(1)
        graph_out = self.graph_branch(x)
        fused = torch.cat([seq_out, graph_out], dim=1)
        return self.head(fused)


class StudentRiskModel(nn.Module):
    def __init__(self, in_dim: int = 5, hidden_dim: int = 32):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(in_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Dropout(0.2),
            nn.Linear(hidden_dim, 2),
        )

    def forward(self, x):
        return self.net(x)


def calculate_metrics(y_true, y_pred, y_prob):
    acc = accuracy_score(y_true, y_pred)
    precision = precision_score(y_true, y_pred)
    recall = recall_score(y_true, y_pred)
    f1 = f1_score(y_true, y_pred)
    auc = roc_auc_score(y_true, y_prob[:, 1])
    cm = confusion_matrix(y_true, y_pred)
    return {
        "accuracy": acc,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "auc": auc,
        "confusion_matrix": cm,
    }


def train_teacher(df: pd.DataFrame, epochs: int, lr: float):
    x = df[FEATURE_COLUMNS].values.astype(np.float32)
    y = df["is_reused"].values.astype(np.int64)

    class_counts = np.bincount(y)
    class_weights = torch.tensor([class_counts[0] / class_counts[1], 1.0], dtype=torch.float32)
    print(f"数据分布: 负样本={class_counts[0]}, 正样本={class_counts[1]}, 权重={class_weights.numpy()}")

    scaler = StandardScaler()
    x_scaled = scaler.fit_transform(x).astype(np.float32)

    x_train, x_val, y_train, y_val = train_test_split(
        x_scaled, y, teststep()

        model.eval()
        with torch.no_grad():
            pred = model(x_val_t).argmax(dim=1)
            acc = (pred == y_val_t).float().mean().item()
        if (epoch + 1) % max(1, epochs // 5) == 0:
            print(f"[teacher] epoch={epoch + 1}/{epochs}, loss={loss.item():.4f}, val_acc={acc:.4f}")

    return model, scaler


def distill_student(teacher: nn.Module, df: pd.DataFrame, epochs: int, lr: float):
    x = df[FEATURE_COLUMNS].values.astype(np.float32)
    scaler = StandardScaler()
    x_scaled = scaler.fit_transform(x).astype(np.float32)

    x_train, x_val = train_test_split(x_scaled, test_size=0.2, random_state=42)
    x_train_t = torch.tensor(x_train)
    x_val_t = torch.tensor(x_val)

    student = StudentRiskModel(in_dim=len(FEATURE_COLUMNS))
    optimizer = torch.optim.Adam(student.parameters(), lr=lr)
    kl = nn.KLDivLoss(reduction="batchmean")

    teacher.eval()
    for epoch in range(epochs):
        student.train()
        with torch.no_grad():
            teacher_logits = teacher(x_train_t)
            teacher_prob = torch.softmax(teacher_logits / 2.0, dim=1)

        student_logits = student(x_train_t)
        student_log_prob = torch.log_softmax(student_logits / 2.0, dim=1)
        loss = kl(student_log_prob, teacher_prob) * (2.0 ** 2)

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        if (epoch + 1) % max(1, epochs // 5) == 0:
            student.eval()
            with torch.no_grad():
                val_logits = student(x_val_t)
                confidence = torch.softmax(val_logits, dim=1)[:, 1].mean().item()
            print(
                f"[student] epoch={epoch + 1}/{epochs}, distill_loss={loss.item():.4f}, mean_risk_prob={confidence:.4f}"
            )

    return student, scaler


def export_onnx(student: nn.Module, output_path: Path):
    output_path.parent.mkdir(parents=True, exist_ok=True)
    dummy = torch.randn(1, len(FEATURE_COLUMNS), dtype=torch.float32)
    torch.onnx.export(
        student,
        dummy,
        output_path.as_posix(),
        input_names=["features"],
        output_names=["logits"],
        dynamic_axes={"features": {0: "batch"}, "logits": {0: "batch"}},
        opset_version=13,
    )
    print(f"onnx exported: {output_path}")


def build_parser():
    parser = argparse.ArgumentParser(description="Offline risk-model training and ONNX export")
    parser.add_argument("--reuse-csv", required=True, help="Path to reuse_pattern dataset csv")
    parser.add_argument("--teacher-epochs", type=int, default=30)
    parser.add_argument("--student-epochs", type=int, default=20)
    parser.add_argument("--learning-rate", type=float, default=1e-3)
    parser.add_argument("--onnx-out", default="artifacts/qs_risk_model.onnx")
    return parser


def load_and_preprocess_data(csv_path: Path) -> pd.DataFrame:
    df = pd.read_csv(csv_path)
    print(f"原始样本数: {len(df)}")
    
    df_clean = df.dropna(subset=["is_reused"])
    dropped_count = len(df) - len(df_clean)
    if dropped_count > 0:
        print(f"删除标签缺失样本: {dropped_count}")
        df = df_clean
    
    for col in FEATURE_COLUMNS:
        if df[col].isnull().any():
            median_val = df[col].median()
            df[col] = df[col].fillna(median_val)
            print(f"填充 {col} 缺失值，中位数: {median_val:.2f}")
    
    print(f"处理后样本数: {len(df)}")
    return df


def main():
    args = build_parser().parse_args()
    reuse_csv = Path(args.reuse_csv)
    if not reuse_csv.exists():
        raise FileNotFoundError(f"dataset not found: {reuse_csv}")

    df = load_and_preprocess_data(reuse_csv)
    teacher, _ = train_teacher(df, epochs=args.teacher_epochs, lr=args.learning_rate)
    student, _ = distill_student(teacher, df, epochs=args.student_epochs, lr=args.learning_rate)
    export_onnx(student, Path(args.onnx_out))


if __name__ == "__main__":
    main()

