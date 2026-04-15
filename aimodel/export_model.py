import torch
import torch.nn as nn
import os

os.environ['PYTHONIOENCODING'] = 'utf-8'

class StudentModel(nn.Module):
    def __init__(self, in_dim=5):
        super().__init__()
        self.fc = nn.Sequential(
            nn.Linear(in_dim, 32),
            nn.ReLU(),
            nn.BatchNorm1d(32),
            nn.Dropout(0.3),
            nn.Linear(32, 16),
            nn.ReLU(),
            nn.BatchNorm1d(16),
            nn.Dropout(0.2),
            nn.Linear(16, 2)
        )
    def forward(self, x):
        return self.fc(x)

student = StudentModel()
student.eval()

output_path = r'e:\Code\project\graduation\aimodel\qs_risk_model.onnx'

torch.onnx.export(
    student,
    torch.randn(1, 5),
    output_path,
    input_names=['features'],
    output_names=['logits'],
    opset_version=18,
    export_params=True,
    do_constant_folding=True,
    verbose=False,
    dynamo=False
)

print('Model exported successfully to:', output_path)
print('File exists:', os.path.exists(output_path))
if os.path.exists(output_path):
    print('File size:', os.path.getsize(output_path), 'bytes')
