#!/usr/bin/env python3
"""통합 seed CSV 데이터를 MySQL에 batch insert하는 스크립트."""
import argparse
import csv
import os
from pathlib import Path
from typing import Any
import pymysql

BATCH_SIZE = 5_000

TABLE_SPECS: list[dict[str, Any]] = [
    {"table":"users","file":"users.csv","columns":["id","email","password","name","role","status","last_active_at","created_at","updated_at","deleted_at"],"nullable_columns":{"last_active_at","deleted_at"}},
    {"table":"wallets","file":"wallets.csv","columns":["id","user_id","balance","event_balance","total_balance","created_at","updated_at"],"nullable_columns":set()},
    {"table":"point_histories","file":"point_histories.csv","columns":["id","wallet_id","point_type","point_history_type","amount","balance_after","log_description","created_at"],"nullable_columns":set()},
    {"table":"billing_keys","file":"billing_keys.csv","columns":["id","user_id","billing_key","card_last4","card_name","pay_method","active","created_at"],"nullable_columns":{"card_last4","card_name","pay_method"}},
    {"table":"cash_orders","file":"cash_orders.csv","columns":["id","user_id","product_type","cash_amount","point_amount","order_number","status","webhook_id","created_at"],"nullable_columns":{"webhook_id"}},
    {"table":"point_orders","file":"point_orders.csv","columns":["id","user_id","plan_type","subscription_amount","order_number","status","created_at"],"nullable_columns":set()},
    {"table":"payments","file":"payments.csv","columns":["id","user_id","amount","status","order_number","pg_transaction_id","webhook_id","refund_reason","paid_at","refunded_at","created_at"],"nullable_columns":{"refund_reason","paid_at","refunded_at"}},
    {"table":"subscriptions","file":"subscriptions.csv","columns":["id","user_id","point_order_id","plan_type","status","start_date","expiry_date","next_billing_date","created_at"],"nullable_columns":{"expiry_date","next_billing_date"}},
    {"table":"artists","file":"artists.csv","columns":["id","owner_user_id","name","artist_type","bio","profile_image_url","status","created_at","updated_at","deleted_at"],"nullable_columns":{"deleted_at"}},
    {"table":"albums","file":"albums.csv","columns":["id","artist_id","title","description","cover_image_url","status","scheduled_publish_at","published_at","track_count","total_duration_sec","created_at","updated_at","deleted_at"],"nullable_columns":{"description","cover_image_url","scheduled_publish_at","published_at","deleted_at"}},
    {"table":"tracks","file":"tracks.csv","columns":["id","owner_user_id","track_type","artist_id","album_id","track_number","title","lyrics","genre","audio_url","duration_sec","featured_artist_text","status","scheduled_publish_at","published_at","play_count","created_at","updated_at","deleted_at"],"nullable_columns":{"artist_id","album_id","track_number","lyrics","featured_artist_text","scheduled_publish_at","published_at","deleted_at"}},
    {"table":"likes","file":"likes.csv","columns":["id","user_id","target_id","target_type","created_at"],"nullable_columns":set()},
    {"table":"follows","file":"follows.csv","columns":["id","artist_id","user_id","notification_enabled","created_at"],"nullable_columns":set()},
    {"table":"playbacks","file":"playbacks.csv","columns":["id","playlist_id","track_id","user_id","session_id","device_id","status","played_duration","started_at","last_played_at","ended_at"],"nullable_columns":{"device_id","ended_at"}},
    {"table":"search_histories","file":"search_histories.csv","columns":["id","user_id","keyword","result_count","created_at"],"nullable_columns":{"user_id","result_count"}},
    {"table":"playlists","file":"playlists.csv","columns":["id","user_id","title","description","deleted","updated_at","deleted_at","created_at"],"nullable_columns":{"description","deleted_at"}},
    {"table":"playlist_tracks","file":"playlist_tracks.csv","columns":["id","playlist_id","track_id","position","created_at"],"nullable_columns":set()},
    {"table":"track_comments","file":"track_comments.csv","columns":["id","user_id","track_id","content","created_at","updated_at","deleted_at"],"nullable_columns":{"deleted_at"}},
    {"table":"artist_applications","file":"artist_applications.csv","columns":["id","requester_user_id","requested_name","artist_type","bio","profile_image_url","status","reviewed_by_admin_id","reviewed_at","rejection_reason","created_at","updated_at"],"nullable_columns":{"bio","profile_image_url","reviewed_by_admin_id","reviewed_at","rejection_reason"}},
    {"table":"album_applications","file":"album_applications.csv","columns":["id","requester_user_id","artist_id","title","description","cover_image_url","publish_delay_days","status","reviewed_by_admin_id","reviewed_at","rejection_reason","created_at","updated_at"],"nullable_columns":{"description","cover_image_url","reviewed_by_admin_id","reviewed_at","rejection_reason"}},
    {"table":"track_applications","file":"track_applications.csv","columns":["id","requester_user_id","track_type","artist_id","album_id","track_number","title","lyrics","genre","audio_url","duration_sec","featured_artist_text","publish_delay_days","status","reviewed_by_admin_id","reviewed_at","rejection_reason","created_at","updated_at"],"nullable_columns":{"artist_id","album_id","track_number","lyrics","featured_artist_text","publish_delay_days","reviewed_by_admin_id","reviewed_at","rejection_reason"}},
]

INT_COLUMNS = {"active","album_id","amount","artist_id","balance","balance_after","cash_amount","deleted","duration_sec","event_balance","id","notification_enabled","owner_user_id","play_count","played_duration","playlist_id","point_amount","point_order_id","position","publish_delay_days","requester_user_id","result_count","reviewed_by_admin_id","subscription_amount","target_id","total_balance","total_duration_sec","track_count","track_id","track_number","user_id","wallet_id"}

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Insert integrated seed CSV files into MySQL.")
    parser.add_argument("--scale", default="smoke", choices=["smoke","test","local","dev"], help="CSV scale directory under scripts/seed/output.")
    parser.add_argument("--input-dir", default="scripts/seed/output", help="Base CSV output directory.")
    parser.add_argument("--batch-size", type=int, default=BATCH_SIZE, help="Batch size for executemany.")
    return parser.parse_args()

def connect():
    return pymysql.connect(host=os.getenv("DB_HOST","127.0.0.1"), port=int(os.getenv("DB_PORT","3306")), user=os.getenv("DB_USER","root"), password=os.getenv("DB_PASSWORD") or None, database=os.getenv("DB_NAME","fivefy_db"), charset="utf8mb4", autocommit=False)

def normalize_value(column: str, value: str, nullable_columns: set[str]):
    if column in nullable_columns and (value == "" or value == r"\N"):
        return None
    if column in INT_COLUMNS and value != "":
        return int(value)
    return value

def build_insert_sql(table: str, columns: list[str]) -> str:
    return f"INSERT INTO `{table}` ({', '.join(f'`{c}`' for c in columns)}) VALUES ({', '.join(['%s'] * len(columns))})"

def read_rows(path: Path, columns: list[str], nullable_columns: set[str]):
    with path.open("r", newline="", encoding="utf-8") as file:
        reader = csv.DictReader(file)
        for row in reader:
            yield tuple(normalize_value(column, row[column], nullable_columns) for column in columns)

def insert_table(connection, input_dir: Path, spec: dict[str, Any], batch_size: int) -> None:
    table = spec["table"]
    print(f"[insert] start table={table}")
    csv_path = input_dir / spec["file"]
    columns = spec["columns"]
    nullable_columns = spec["nullable_columns"]
    if not csv_path.exists():
        raise FileNotFoundError(f"CSV file not found: {csv_path}")
    sql = build_insert_sql(table, columns)
    inserted = 0
    batch = []
    with connection.cursor() as cursor:
        for row in read_rows(csv_path, columns, nullable_columns):
            batch.append(row)
            if len(batch) >= batch_size:
                cursor.executemany(sql, batch)
                inserted += len(batch)
                batch.clear()
                print(f"[insert] {table}: {inserted}")
        if batch:
            cursor.executemany(sql, batch)
            inserted += len(batch)
    print(f"[insert] {table}: completed total={inserted}")

def main() -> None:
    args = parse_args()
    input_dir = Path(args.input_dir) / args.scale
    print(f"[insert] input_dir={input_dir}")
    print(f"[insert] batch_size={args.batch_size}")
    connection = connect()
    try:
        with connection.cursor() as cursor:
            cursor.execute("SET SESSION foreign_key_checks = 0")
        for spec in TABLE_SPECS:
            insert_table(connection, input_dir, spec, args.batch_size)
        with connection.cursor() as cursor:
            cursor.execute("SET SESSION foreign_key_checks = 1")
        connection.commit()
        print("[insert] all tables completed.")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

if __name__ == "__main__":
    main()
