import argparse
from pathlib import Path

import pandas as pd
import pymysql


def parse_args():
    p = argparse.ArgumentParser(description="Export reuse_pattern features from MySQL")
    p.add_argument("--host", default="127.0.0.1")
    p.add_argument("--port", type=int, default=3306)
    p.add_argument("--user", default="root")
    p.add_argument("--password", default="123456")
    p.add_argument("--database", default="yx_ai_feature")
    p.add_argument("--output", default="data/reuse_pattern.csv")
    return p.parse_args()


def main():
    args = parse_args()
    conn = pymysql.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database=args.database,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    sql = """
        SELECT
          qs_id,
          scan_count,
          time_variance,
          location_variance,
          device_count,
          ip_count,
          is_reused,
          model_version,
          update_time
        FROM reuse_pattern
    """
    df = pd.read_sql(sql, conn)
    conn.close()

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(out, index=False)
    print(f"exported rows={len(df)} to {out}")


if __name__ == "__main__":
    main()

