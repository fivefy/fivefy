#!/usr/bin/env python3
"""통합 성능 검증용 CSV seed 생성기."""
import argparse
import csv
import random
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Optional

@dataclass(frozen=True)
class ScaleConfig:
    users: int
    wallets: int
    point_histories: int
    billing_keys: int
    cash_orders: int
    point_orders: int
    payments: int
    subscriptions: int
    artists: int
    albums: int
    tracks: int
    likes: int
    follows: int
    playbacks: int
    search_histories: int
    playlists: int
    playlist_tracks: int
    track_comments: int
    artist_applications: int
    album_applications: int
    track_applications: int

SCALE_CONFIGS = {
    "smoke": ScaleConfig(1_000,1_000,2_000,500,2_000,1_000,1_500,800,1_000,3_000,10_000,5_000,2_000,30_000,3_000,1_000,5_000,30_000,1_000,2_000,3_000),
    "test": ScaleConfig(10_000,10_000,20_000,5_000,20_000,10_000,15_000,8_000,10_000,30_000,100_000,50_000,20_000,300_000,30_000,10_000,50_000,300_000,10_000,20_000,30_000),
    "local": ScaleConfig(100_000,100_000,200_000,50_000,200_000,100_000,150_000,80_000,100_000,300_000,1_000_000,500_000,200_000,3_000_000,300_000,100_000,500_000,3_000_000,100_000,200_000,300_000),
    "dev": ScaleConfig(500_000,500_000,1_000_000,250_000,1_000_000,500_000,750_000,400_000,500_000,1_500_000,10_000_000,3_000_000,1_000_000,30_000_000,1_000_000,500_000,3_000_000,30_000_000,300_000,600_000,1_000_000),
}

BASE_DATE = datetime(2024, 1, 1, 0, 0, 0)
NOW = datetime(2026, 4, 27, 0, 0, 0)

def random_datetime_between(start: datetime, end: datetime) -> datetime:
    seconds = max(0, int((end - start).total_seconds()))
    return start + timedelta(seconds=random.randint(0, seconds))

def format_dt(value: Optional[datetime]) -> str:
    return "" if value is None else value.strftime("%Y-%m-%d %H:%M:%S")

def nullable(value: Optional[object]) -> object:
    return "" if value is None else value

def weighted_choice(items: list[tuple[str, int]]) -> str:
    return random.choices([i[0] for i in items], weights=[i[1] for i in items], k=1)[0]

def write_csv(path: Path, headers: list[str], row_generator) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(headers)
        for row in row_generator:
            writer.writerow(row)

def generate_users(count: int):
    for user_id in range(1, count + 1):
        role = "ADMIN" if user_id <= max(1, count // 200) else "USER"
        status = weighted_choice([("ACTIVE",95),("SUSPENDED",3),("BANNED",1),("DELETED",1)])
        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        deleted_at = random_datetime_between(updated_at, NOW) if status == "DELETED" else None
        last_active_at = random_datetime_between(created_at, NOW) if status == "ACTIVE" else None
        yield [user_id, f"user{user_id}@fivefy.local", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", f"User {user_id}", role, status, format_dt(last_active_at), format_dt(created_at), format_dt(updated_at), format_dt(deleted_at)]

def generate_wallets(count: int):
    for wallet_id in range(1, count + 1):
        balance = random.randint(0, 100_000)
        event_balance = random.randint(0, 30_000)
        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        yield [wallet_id, wallet_id, balance, event_balance, balance + event_balance, format_dt(created_at), format_dt(updated_at)]

def generate_point_histories(count: int, wallet_count: int):
    for history_id in range(1, count + 1):
        created_at = random_datetime_between(BASE_DATE, NOW)
        yield [history_id, random.randint(1, wallet_count), weighted_choice([("PAID",70),("FREE",30)]), weighted_choice([("CHARGE",50),("USE",40),("REFUND",10)]), random.randint(100,50_000), random.randint(0,200_000), f"Seed point history {history_id}", format_dt(created_at)]

def cash_product_amount(product_type: str) -> tuple[int, int]:
    return {"PRODUCT_1": (1000,1000), "PRODUCT_2": (2000,2500), "PRODUCT_3": (0,150), "PRODUCT_4": (3000,4500), "PRODUCT_4_RECURRING": (1000,1500)}[product_type]

def subscription_amount(plan_type: str) -> int:
    return 0 if plan_type == "FREE" else 50

def generate_billing_keys(count: int, user_count: int):
    for billing_key_id in range(1, count + 1):
        created_at = random_datetime_between(BASE_DATE, NOW)
        yield [billing_key_id, random.randint(1,user_count), f"billing-key-{billing_key_id}", f"{random.randint(0,9999):04d}", weighted_choice([("HYUNDAI",25),("SAMSUNG",25),("KB",20),("SHINHAN",20),("KAKAO_PAY",10)]), weighted_choice([("CARD",80),("KAKAO_PAY",20)]), 1 if random.random() < 0.9 else 0, format_dt(created_at)]

def generate_cash_orders(count: int, user_count: int):
    for order_id in range(1, count + 1):
        product_type = weighted_choice([("PRODUCT_1",35),("PRODUCT_2",30),("PRODUCT_3",10),("PRODUCT_4",15),("PRODUCT_4_RECURRING",10)])
        cash_amount, point_amount = cash_product_amount(product_type)
        status = weighted_choice([("PENDING",20),("SUCCESS",70),("REFUNDED",10)])
        created_at = random_datetime_between(BASE_DATE, NOW)
        yield [order_id, random.randint(1,user_count), product_type, cash_amount, point_amount, f"CASH-{order_id}", status, nullable(f"cash-webhook-{order_id}" if status in ("SUCCESS","REFUNDED") else None), format_dt(created_at)]

def generate_point_orders(count: int, user_count: int):
    for order_id in range(1, count + 1):
        plan_type = weighted_choice([("FREE",20),("RECURRING",80)])
        status = weighted_choice([("PENDING",15),("SUCCESS",75),("REFUNDED",10)])
        created_at = random_datetime_between(BASE_DATE, NOW)
        yield [order_id, random.randint(1,user_count), plan_type, subscription_amount(plan_type), f"POINT-{order_id}", status, format_dt(created_at)]

def generate_payments(count: int, user_count: int, cash_order_count: int):
    for payment_id in range(1, count + 1):
        cash_order_id = random.randint(1, cash_order_count)
        status = weighted_choice([("REQUESTED",10),("HOLD",5),("APPROVED",10),("COMPLETED",60),("FAILED",5),("CANCELED",5),("REFUNDED",5)])
        created_at = random_datetime_between(BASE_DATE, NOW)
        paid_at = random_datetime_between(created_at, NOW) if status in ("COMPLETED","REFUNDED") else None
        refunded_at = random_datetime_between(paid_at or created_at, NOW) if status == "REFUNDED" else None
        yield [payment_id, random.randint(1,user_count), random.choice([0,1000,2000,3000]), status, f"CASH-{cash_order_id}", f"pg-tx-{payment_id}", f"payment-webhook-{payment_id}", nullable("Seed refund reason" if status == "REFUNDED" else None), format_dt(paid_at), format_dt(refunded_at), format_dt(created_at)]

def generate_subscriptions(count: int, user_count: int, point_order_count: int):
    for subscription_id in range(1, count + 1):
        plan_type = weighted_choice([("FREE",20),("RECURRING",80)])
        status = weighted_choice([("FREE",10),("ACTIVE",65),("INACTIVE",10),("EXPIRE",5),("CANCELED",8),("REFUND",2)])
        start_date = random_datetime_between(BASE_DATE, NOW)
        expiry_date = start_date + (timedelta(days=3) if plan_type == "FREE" else timedelta(days=30))
        next_billing_date = expiry_date if plan_type == "RECURRING" and status == "ACTIVE" else None
        created_at = random_datetime_between(BASE_DATE, start_date)
        yield [subscription_id, random.randint(1,user_count), random.randint(1,point_order_count), plan_type, status, format_dt(start_date), format_dt(expiry_date), format_dt(next_billing_date), format_dt(created_at)]

def generate_artists(count: int, user_count: int):
    for artist_id in range(1, count + 1):
        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        yield [artist_id, random.randint(1,user_count), f"Artist {artist_id}", weighted_choice([("SOLO",90),("COLLABORATION",10)]), f"Bio for artist {artist_id}", f"https://cdn.fivefy.local/artists/{artist_id}.jpg", weighted_choice([("ACTIVE",90),("INACTIVE",10)]), format_dt(created_at), format_dt(updated_at), format_dt(random_datetime_between(updated_at, NOW) if random.random()<0.05 else None)]

def generate_album_artist_ids(album_count: int, artist_count: int) -> list[int]:
    return [random.randint(1, artist_count) for _ in range(album_count)]

def generate_albums(count: int, album_artist_ids: list[int]):
    for album_id in range(1, count + 1):
        status = weighted_choice([("PUBLISHED",70),("UNPUBLISHED",20),("BLOCKED",10)])
        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        published_at = random_datetime_between(created_at, NOW) if status == "PUBLISHED" else None
        scheduled_publish_at = None if status == "PUBLISHED" else created_at + timedelta(days=random.randint(1,7))
        artist_id = album_artist_ids[album_id - 1]
        yield [album_id, artist_id, f"Album {album_id}", f"Description for album {album_id}", f"https://cdn.fivefy.local/albums/{album_id}.jpg", status, format_dt(scheduled_publish_at), format_dt(published_at), 0, 0, format_dt(created_at), format_dt(updated_at), format_dt(random_datetime_between(updated_at, NOW) if random.random()<0.05 else None)]

def generate_tracks(count: int, user_count: int, artist_count: int, album_count: int, album_artist_ids: list[int]):
    used_official_track_numbers = set()

    for track_id in range(1, count + 1):
        track_type = weighted_choice([("OFFICIAL_RELEASE",70),("FREE_CREATION",30)])
        status = weighted_choice([("PUBLISHED",70),("UNPUBLISHED",20),("BLOCKED",10)])
        owner_user_id = random.randint(1,user_count)

        if track_type == "OFFICIAL_RELEASE":
            while True:
                album_id = random.randint(1, album_count)
                artist_id = album_artist_ids[album_id - 1]
                track_number = random.randint(1, 20)
                track_number_key = (album_id, track_number)

                if track_number_key not in used_official_track_numbers:
                    used_official_track_numbers.add(track_number_key)
                    break

            featured_artist_text = None if random.random()<0.8 else f"Featured Artist {random.randint(1,1000)}"
        else:
            artist_id = album_id = track_number = featured_artist_text = None

        created_at=random_datetime_between(BASE_DATE,NOW)
        updated_at=random_datetime_between(created_at,NOW)
        published_at=random_datetime_between(created_at,NOW) if status=="PUBLISHED" else None
        scheduled_publish_at=None if status=="PUBLISHED" else created_at+timedelta(days=random.randint(1,7))

        yield [track_id, owner_user_id, track_type, nullable(artist_id), nullable(album_id), nullable(track_number), f"Track {track_id}", f"Lyrics for track {track_id}", f"GENRE_{random.randint(1,20)}", f"https://cdn.fivefy.local/audio/{track_id}.mp3", random.randint(60,420), nullable(featured_artist_text), status, format_dt(scheduled_publish_at), format_dt(published_at), int(random.paretovariate(1.5)*100), format_dt(created_at), format_dt(updated_at), format_dt(random_datetime_between(updated_at,NOW) if random.random()<0.05 else None)]

def generate_likes(count: int, user_count: int, track_count: int):
    for like_id in range(1, count + 1):
        user_id = ((like_id - 1) % user_count) + 1
        target_id = (((like_id - 1) // user_count) % track_count) + 1
        target_type = "TRACK"
        created_at = random_datetime_between(BASE_DATE, NOW)

        yield [like_id, user_id, target_id, target_type, format_dt(created_at)]

def generate_follows(count: int, user_count: int, artist_count: int):
    for follow_id in range(1, count + 1):
        user_id = ((follow_id - 1) % user_count) + 1
        artist_id = (((follow_id - 1) // user_count) % artist_count) + 1
        notification_enabled = 1 if random.random() < 0.8 else 0
        created_at = random_datetime_between(BASE_DATE, NOW)

        yield [follow_id, artist_id, user_id, notification_enabled, format_dt(created_at)]

def generate_playbacks(count: int, user_count: int, playlist_count: int, track_count: int):
    for playback_id in range(1, count + 1):
        status = weighted_choice([("PLAYING", 10), ("PAUSED", 10), ("STOPPED", 5), ("SKIPPED", 5), ("COMPLETED", 70)])
        started_at = random_datetime_between(BASE_DATE, NOW)
        played_duration = random.randint(10, 420)
        last_played_at = started_at + timedelta(seconds=random.randint(1, played_duration))
        ended_at = started_at + timedelta(seconds=played_duration) if status in ("STOPPED", "SKIPPED", "COMPLETED") else None
        device_id = None if random.random() < 0.2 else f"device-{random.randint(1, 10000)}"
        yield [playback_id, random.randint(1, playlist_count), random.randint(1, track_count), random.randint(1, user_count), f"seed-session-{playback_id}", nullable(device_id), status, played_duration, format_dt(started_at), format_dt(last_played_at), format_dt(ended_at)]

def generate_search_histories(count: int, user_count: int):
    keywords = ["love","night","summer","dream","rain","dance","blue","star","moon","city","piano","rock","jazz","lofi","classic"]
    for search_history_id in range(1, count + 1):
        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        yield [search_history_id, random.randint(1, user_count), random.choice(keywords), random.randint(0, 500), format_dt(created_at)]

def generate_playlists(count: int, user_count: int):
    for playlist_id in range(1, count + 1):
        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        deleted = 1 if random.random() < 0.05 else 0
        deleted_at = random_datetime_between(updated_at, NOW) if deleted else None
        yield [playlist_id, random.randint(1, user_count), f"Playlist {playlist_id}", f"Seed playlist description {playlist_id}", deleted, format_dt(updated_at), format_dt(deleted_at), format_dt(created_at)]

def generate_playlist_tracks(count: int, playlist_count: int, track_count: int):
    playlist_track_id = 1
    base_per_playlist = count // playlist_count
    remainder = count % playlist_count

    for playlist_id in range(1, playlist_count + 1):
        track_limit = base_per_playlist + (1 if playlist_id <= remainder else 0)

        for position in range(1, track_limit + 1):
            track_id = ((playlist_id - 1) * track_limit + position - 1) % track_count + 1
            created_at = random_datetime_between(BASE_DATE, NOW)

            yield [playlist_track_id, playlist_id, track_id, position, format_dt(created_at)]
            playlist_track_id += 1

def generate_track_comments(count: int, user_count: int, track_count: int):
    hot_track_count=min(100,track_count); hot_comment_limit=count//2
    for comment_id in range(1,count+1):
        created_at=random_datetime_between(BASE_DATE,NOW); updated_at=random_datetime_between(created_at,NOW)
        yield [comment_id, random.randint(1,user_count), random.randint(1,hot_track_count if comment_id<=hot_comment_limit else track_count), f"Track comment content {comment_id}", format_dt(created_at), format_dt(updated_at), format_dt(random_datetime_between(updated_at,NOW) if random.random()<0.10 else None)]

def generate_artist_applications(count: int, user_count: int):
    for application_id in range(1,count+1):
        requester_user_id=random.randint(1,user_count); status=weighted_choice([("PENDING",30),("APPROVED",40),("REJECTED",30)])
        created_at=random_datetime_between(BASE_DATE,NOW); updated_at=random_datetime_between(created_at,NOW)
        reviewed_by_admin_id=reviewed_at=rejection_reason=None
        if status in ("APPROVED","REJECTED"):
            reviewed_by_admin_id=random.randint(1,max(1,user_count//200)); reviewed_at=random_datetime_between(created_at,NOW)
        if status=="REJECTED": rejection_reason="등록 신청 기준에 부합하지 않습니다."
        yield [application_id, requester_user_id, f"Requested Artist {application_id}", weighted_choice([("SOLO",90),("COLLABORATION",10)]), f"Requested artist bio {application_id}", f"https://cdn.fivefy.local/artist-applications/{application_id}.jpg", status, nullable(reviewed_by_admin_id), format_dt(reviewed_at), nullable(rejection_reason), format_dt(created_at), format_dt(updated_at)]

def generate_album_applications(count: int, user_count: int, artist_count: int):
    for application_id in range(1,count+1):
        requester_user_id=random.randint(1,user_count); status=weighted_choice([("PENDING",30),("APPROVED",40),("REJECTED",30)])
        created_at=random_datetime_between(BASE_DATE,NOW); updated_at=random_datetime_between(created_at,NOW)
        reviewed_by_admin_id=reviewed_at=rejection_reason=None
        if status in ("APPROVED","REJECTED"):
            reviewed_by_admin_id=random.randint(1,max(1,user_count//200)); reviewed_at=random_datetime_between(created_at,NOW)
        if status=="REJECTED": rejection_reason="앨범 등록 신청 기준에 부합하지 않습니다."
        yield [application_id, requester_user_id, random.randint(1,artist_count), f"Requested Album {application_id}", f"Requested album description {application_id}", f"https://cdn.fivefy.local/album-applications/{application_id}.jpg", random.randint(0,7), status, nullable(reviewed_by_admin_id), format_dt(reviewed_at), nullable(rejection_reason), format_dt(created_at), format_dt(updated_at)]

def generate_track_applications(count: int, user_count: int, artist_count: int, album_count: int, album_artist_ids: list[int]):
    used_official_track_numbers = set()
    used_official_titles = set()

    for application_id in range(1,count+1):
        requester_user_id=random.randint(1,user_count)
        track_type=weighted_choice([("OFFICIAL_RELEASE",70),("FREE_CREATION",30)])
        status=weighted_choice([("PENDING",30),("APPROVED",40),("REJECTED",30)])
        title = f"Requested Track {application_id}"

        if track_type=="OFFICIAL_RELEASE":
            while True:
                album_id=random.randint(1,album_count)
                artist_id=album_artist_ids[album_id - 1]
                track_number=random.randint(1,20)

                track_number_key = (
                    requester_user_id,
                    artist_id,
                    album_id,
                    track_number
                )
                title_key = (
                    requester_user_id,
                    artist_id,
                    album_id,
                    title
                )

                if (
                        track_number_key not in used_official_track_numbers
                        and title_key not in used_official_titles
                ):
                    used_official_track_numbers.add(track_number_key)
                    used_official_titles.add(title_key)
                    break

            featured_artist_text=None if random.random()<0.8 else f"Featured Artist {random.randint(1,1000)}"
            publish_delay_days=random.randint(0,7)
        else:
            artist_id=album_id=track_number=featured_artist_text=publish_delay_days=None

        created_at=random_datetime_between(BASE_DATE,NOW)
        updated_at=random_datetime_between(created_at,NOW)
        reviewed_by_admin_id=reviewed_at=rejection_reason=None

        if status in ("APPROVED","REJECTED"):
            reviewed_by_admin_id=random.randint(1,max(1,user_count//200))
            reviewed_at=random_datetime_between(created_at,NOW)

        if status=="REJECTED":
            rejection_reason="트랙 등록 신청 기준에 부합하지 않습니다."

        yield [application_id, requester_user_id, track_type, nullable(artist_id), nullable(album_id), nullable(track_number), title, f"Requested lyrics {application_id}", f"GENRE_{random.randint(1,20)}", f"https://cdn.fivefy.local/application-audio/{application_id}.mp3", random.randint(60,420), nullable(featured_artist_text), nullable(publish_delay_days), status, nullable(reviewed_by_admin_id), format_dt(reviewed_at), nullable(rejection_reason), format_dt(created_at), format_dt(updated_at)]

def generate_all(config: ScaleConfig, output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    album_artist_ids = generate_album_artist_ids(config.albums, config.artists)
    write_csv(output_dir / "users.csv", ["id","email","password","name","role","status","last_active_at","created_at","updated_at","deleted_at"], generate_users(config.users))
    write_csv(output_dir / "wallets.csv", ["id","user_id","balance","event_balance","total_balance","created_at","updated_at"], generate_wallets(config.wallets))
    write_csv(output_dir / "point_histories.csv", ["id","wallet_id","point_type","point_history_type","amount","balance_after","log_description","created_at"], generate_point_histories(config.point_histories, config.wallets))
    write_csv(output_dir / "billing_keys.csv", ["id","user_id","billing_key","card_last4","card_name","pay_method","active","created_at"], generate_billing_keys(config.billing_keys, config.users))
    write_csv(output_dir / "cash_orders.csv", ["id","user_id","product_type","cash_amount","point_amount","order_number","status","webhook_id","created_at"], generate_cash_orders(config.cash_orders, config.users))
    write_csv(output_dir / "point_orders.csv", ["id","user_id","plan_type","subscription_amount","order_number","status","created_at"], generate_point_orders(config.point_orders, config.users))
    write_csv(output_dir / "payments.csv", ["id","user_id","amount","status","order_number","pg_transaction_id","webhook_id","refund_reason","paid_at","refunded_at","created_at"], generate_payments(config.payments, config.users, config.cash_orders))
    write_csv(output_dir / "subscriptions.csv", ["id","user_id","point_order_id","plan_type","status","start_date","expiry_date","next_billing_date","created_at"], generate_subscriptions(config.subscriptions, config.users, config.point_orders))
    write_csv(output_dir / "artists.csv", ["id","owner_user_id","name","artist_type","bio","profile_image_url","status","created_at","updated_at","deleted_at"], generate_artists(config.artists, config.users))
    write_csv(output_dir / "albums.csv", ["id","artist_id","title","description","cover_image_url","status","scheduled_publish_at","published_at","track_count","total_duration_sec","created_at","updated_at","deleted_at"], generate_albums(config.albums, album_artist_ids))
    write_csv(output_dir / "tracks.csv", ["id","owner_user_id","track_type","artist_id","album_id","track_number","title","lyrics","genre","audio_url","duration_sec","featured_artist_text","status","scheduled_publish_at","published_at","play_count","created_at","updated_at","deleted_at"], generate_tracks(config.tracks, config.users, config.artists, config.albums, album_artist_ids))
    write_csv(output_dir / "likes.csv", ["id","user_id","target_id","target_type","created_at"], generate_likes(config.likes, config.users, config.tracks))
    write_csv(output_dir / "follows.csv", ["id","artist_id","user_id","notification_enabled","created_at"], generate_follows(config.follows, config.users, config.artists))
    write_csv(output_dir / "playbacks.csv", ["id","playlist_id","track_id","user_id","session_id","device_id","status","played_duration","started_at","last_played_at","ended_at"], generate_playbacks(config.playbacks, config.users, config.playlists, config.tracks))
    write_csv(output_dir / "search_histories.csv", ["id","user_id","keyword","result_count","created_at"], generate_search_histories(config.search_histories, config.users))
    write_csv(output_dir / "playlists.csv", ["id","user_id","title","description","deleted","updated_at","deleted_at","created_at"], generate_playlists(config.playlists, config.users))
    write_csv(output_dir / "playlist_tracks.csv", ["id","playlist_id","track_id","position","created_at"], generate_playlist_tracks(config.playlist_tracks, config.playlists, config.tracks))
    write_csv(output_dir / "track_comments.csv", ["id","user_id","track_id","content","created_at","updated_at","deleted_at"], generate_track_comments(config.track_comments, config.users, config.tracks))
    write_csv(output_dir / "artist_applications.csv", ["id","requester_user_id","requested_name","artist_type","bio","profile_image_url","status","reviewed_by_admin_id","reviewed_at","rejection_reason","created_at","updated_at"], generate_artist_applications(config.artist_applications, config.users))
    write_csv(output_dir / "album_applications.csv", ["id","requester_user_id","artist_id","title","description","cover_image_url","publish_delay_days","status","reviewed_by_admin_id","reviewed_at","rejection_reason","created_at","updated_at"], generate_album_applications(config.album_applications, config.users, config.artists))
    write_csv(output_dir / "track_applications.csv", ["id","requester_user_id","track_type","artist_id","album_id","track_number","title","lyrics","genre","audio_url","duration_sec","featured_artist_text","publish_delay_days","status","reviewed_by_admin_id","reviewed_at","rejection_reason","created_at","updated_at"], generate_track_applications(config.track_applications, config.users, config.artists, config.albums, album_artist_ids))

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate integrated seed CSV files for performance testing.")
    parser.add_argument("--scale", choices=SCALE_CONFIGS.keys(), default="smoke", help="Dataset scale.")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for reproducible data generation.")
    parser.add_argument("--output-dir", type=str, default="scripts/seed/output", help="CSV output directory.")
    return parser.parse_args()

def main() -> None:
    args = parse_args(); random.seed(args.seed)
    output_dir = Path(args.output_dir) / args.scale
    config = SCALE_CONFIGS[args.scale]
    print(f"[seed] scale={args.scale}")
    print(f"[seed] output_dir={output_dir}")
    print(f"[seed] config={config}")
    generate_all(config, output_dir)
    print("[seed] CSV generation completed.")

if __name__ == "__main__":
    main()
