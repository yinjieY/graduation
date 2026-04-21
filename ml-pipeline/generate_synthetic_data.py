import numpy as np
import pandas as pd
import pymysql
from datetime import datetime, timedelta
import random

N_SAMPLES = 8000  # 更大更稳
FRAUD_RATIO = 0.25  # 真实业务风险一般更少

def add_noise(values, noise_level=0.1):
    noise = np.random.normal(0, noise_level * np.mean(values), len(values))
    return np.maximum(values + noise, 0)

def add_missing_values(df, missing_rate=0.03):
    for col in df.columns:
        if col not in ['qs_id', 'is_reused', 'create_time', 'update_time']:
            mask = np.random.random(len(df)) < missing_rate
            df.loc[mask, col] = np.nan
    return df

def add_outliers(df, outlier_rate=0.01):
    numeric_cols = ['scan_count', 'time_variance', 'location_variance', 'device_count', 'ip_count']
    for col in numeric_cols:
        outlier_mask = np.random.random(len(df)) < outlier_rate
        if outlier_mask.any():
            if col in ['scan_count', 'device_count', 'ip_count']:
                df.loc[outlier_mask, col] = np.random.randint(80, 400, outlier_mask.sum())
            else:
                df.loc[outlier_mask, col] = np.random.uniform(8, 40, outlier_mask.sum())
    return df

def save_to_db(df, db_name="yx_ai_feature", table_name="reuse_pattern"):
    conn = pymysql.connect(
        host="localhost",
        user="root",
        password="123456",
        database=db_name,
        charset="utf8mb4"
    )

    drop_table_sql = f"DROP TABLE IF EXISTS {table_name}"
    create_table_sql = f"""
    CREATE TABLE {table_name} (
        id INT AUTO_INCREMENT PRIMARY KEY,
        qs_id VARCHAR(32) NOT NULL,
        scan_count INT NULL,
        time_variance FLOAT NULL,
        location_variance FLOAT NULL,
        device_count INT NULL,
        ip_count INT NULL,
        is_reused TINYINT(1) NOT NULL,
        model_version VARCHAR(20) NOT NULL DEFAULT 'v1.0',
        create_time DATETIME NOT NULL,
        update_time DATETIME NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """

    with conn.cursor() as cursor:
        cursor.execute(drop_table_sql)
        cursor.execute(create_table_sql)

        insert_sql = f"""
        INSERT INTO {table_name}
        (qs_id, scan_count, time_variance, location_variance, device_count, ip_count, is_reused, create_time, update_time)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        for _, row in df.iterrows():
            cursor.execute(insert_sql, (
                row['qs_id'],
                int(row['scan_count']) if pd.notna(row['scan_count']) else None,
                float(row['time_variance']) if pd.notna(row['time_variance']) else None,
                float(row['location_variance']) if pd.notna(row['location_variance']) else None,
                int(row['device_count']) if pd.notna(row['device_count']) else None,
                int(row['ip_count']) if pd.notna(row['ip_count']) else None,
                row['is_reused'],
                row['create_time'],
                row['update_time']
            ))

    conn.commit()
    conn.close()
    print(f"✓ 数据已保存到 {db_name}.{table_name}")

np.random.seed(42)

n_normal = int(N_SAMPLES * (1 - FRAUD_RATIO))
normal_scan_counts = np.random.poisson(lam=4, size=n_normal)
normal_time_var = np.random.exponential(scale=0.6, size=n_normal)
normal_loc_var = np.random.uniform(0, 0.7, size=n_normal)
normal_device_counts = np.random.choice([1,2,3,4,5], p=[0.65, 0.2, 0.1, 0.04, 0.01], size=n_normal)
normal_ip_counts = np.random.choice([1,2,3,4,5], p=[0.7, 0.2, 0.07, 0.02, 0.01], size=n_normal)

normal_data = pd.DataFrame({
    'qs_id': ['QS' + str(np.random.randint(100000, 999999)) + 'N' + str(i).zfill(4) for i in range(n_normal)],
    'scan_count': add_noise(normal_scan_counts, 0.25),
    'time_variance': add_noise(normal_time_var, 0.2),
    'location_variance': add_noise(normal_loc_var, 0.2),
    'device_count': add_noise(normal_device_counts, 0.15),
    'ip_count': add_noise(normal_ip_counts, 0.15),
    'is_reused': 0,
    'create_time': [datetime.now() - timedelta(hours=np.random.randint(0, 24*90)) for _ in range(n_normal)],
    'update_time': [datetime.now() for _ in range(n_normal)]
})

n_fraud = N_SAMPLES - n_normal
fraud_scan_counts = np.random.poisson(lam=10, size=n_fraud)
fraud_time_var = np.random.exponential(scale=1.8, size=n_fraud)
fraud_loc_var = np.random.uniform(0.4, 2.0, size=n_fraud)
fraud_device_counts = np.random.choice([3,4,5,6,7,8,9,10], p=[0.05,0.1,0.2,0.25,0.2,0.1,0.07,0.03], size=n_fraud)
fraud_ip_counts = np.random.choice([3,4,5,6,7,8,9,12], p=[0.05,0.1,0.2,0.25,0.2,0.1,0.07,0.03], size=n_fraud)

fraud_data = pd.DataFrame({
    'qs_id': ['QS' + str(np.random.randint(100000, 999999)) + 'F' + str(i).zfill(4) for i in range(n_fraud)],
    'scan_count': add_noise(fraud_scan_counts, 0.3),
    'time_variance': add_noise(fraud_time_var, 0.25),
    'location_variance': add_noise(fraud_loc_var, 0.25),
    'device_count': add_noise(fraud_device_counts, 0.2),
    'ip_count': add_noise(fraud_ip_counts, 0.2),
    'is_reused': 1,
    'create_time': [datetime.now() - timedelta(hours=np.random.randint(0, 24*90)) for _ in range(n_fraud)],
    'update_time': [datetime.now() for _ in range(n_fraud)]
})

df = pd.concat([normal_data, fraud_data]).sample(frac=1).reset_index(drop=True)
df = add_missing_values(df, missing_rate=0.04)
df = add_outliers(df, outlier_rate=0.012)

df['scan_count'] = df['scan_count'].apply(lambda x: max(0, int(x)) if pd.notna(x) else None)
df['device_count'] = df['device_count'].apply(lambda x: max(1, int(x)) if pd.notna(x) else None)
df['ip_count'] = df['ip_count'].apply(lambda x: max(1, int(x)) if pd.notna(x) else None)

print("="*60)
print("          生成数据统计")
print("="*60)
print(f"总样本数: {len(df)}")
print(f"正常样本: {len(df[df['is_reused'] == 0])} ({(1-FRAUD_RATIO)*100:.0f}%)")
print(f"套码样本: {len(df[df['is_reused'] == 1])} ({FRAUD_RATIO*100:.0f}%)")
print(f"\n缺失值统计:")
print(df.isnull().sum())
print("\n特征统计:")
print(df[['scan_count', 'time_variance', 'location_variance', 'device_count', 'ip_count']].describe())

try:
    save_to_db(df)
    print("\n✓ 数据生成成功！")
except Exception as e:
    print(f"\n✗ 保存失败: {e}")
    df.to_csv('synthetic_training_data.csv', index=False)
    print("✓ 数据已保存到 synthetic_training_data.csv")