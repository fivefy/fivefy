#!/usr/bin/env python3
"""
통합 성능 검증용 CSV seed 생성기.

목적:
- integrated 데이터셋 CSV 생성
- scale 옵션으로 데이터 규모 증량
- random seed 고정으로 동일 데이터 재현
"""

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
    artists: int
    albums: int
    tracks: int
    track_comments: int
    artist_applications: int
    album_applications: int
    track_applications: int


SCALE_CONFIGS = {
    "smoke": ScaleConfig(
        users=1_000,
        wallets=1_000,
        point_histories=2_000,
        artists=1_000,
        albums=3_000,
        tracks=10_000,
        track_comments=30_000,
        artist_applications=1_000,
        album_applications=2_000,
        track_applications=3_000,
    ),
    "test": ScaleConfig(
        users=10_000,
        wallets=10_000,
        point_histories=20_000,
        artists=10_000,
        albums=30_000,
        tracks=100_000,
        track_comments=300_000,
        artist_applications=10_000,
        album_applications=20_000,
        track_applications=30_000,
    ),
    "local": ScaleConfig(
        users=100_000,
        wallets=100_000,
        point_histories=200_000,
        artists=100_000,
        albums=300_000,
        tracks=1_000_000,
        track_comments=3_000_000,
        artist_applications=100_000,
        album_applications=200_000,
        track_applications=300_000,
    ),
    "dev": ScaleConfig(
        users=500_000,
        wallets=500_000,
        point_histories=1_000_000,
        artists=500_000,
        albums=1_500_000,
        tracks=10_000_000,
        track_comments=30_000_000,
        artist_applications=300_000,
        album_applications=600_000,
        track_applications=1_000_000,
    ),
}


BASE_DATE = datetime(2024, 1, 1, 0, 0, 0)
NOW = datetime(2026, 4, 27, 0, 0, 0)


def random_datetime_between(start: datetime, end: datetime) -> datetime:
    seconds = int((end - start).total_seconds())
    return start + timedelta(seconds=random.randint(0, seconds))


def format_dt(value: Optional[datetime]) -> str:
    if value is None:
        return ""
    return value.strftime("%Y-%m-%d %H:%M:%S")


def nullable(value: Optional[object]) -> object:
    return "" if value is None else value


def weighted_choice(items: list[tuple[str, int]]) -> str:
    values = [item[0] for item in items]
    weights = [item[1] for item in items]
    return random.choices(values, weights=weights, k=1)[0]


def ensure_output_dir(output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)


def write_csv(path: Path, headers: list[str], row_generator) -> None:
    with path.open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(headers)

        for row in row_generator:
            writer.writerow(row)


def generate_users(count: int):
    for user_id in range(1, count + 1):
        role = "ADMIN" if user_id <= max(1, count // 200) else "USER"
        status = weighted_choice([
            ("ACTIVE", 95),
            ("SUSPENDED", 3),
            ("BANNED", 1),
            ("DELETED", 1),
        ])

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        deleted_at = random_datetime_between(updated_at, NOW) if status == "DELETED" else None
        last_active_at = random_datetime_between(created_at, NOW) if status == "ACTIVE" else None

        yield [
            user_id,
            f"user{user_id}@fivefy.local",
            "$2a$10$seededPasswordHashForPerformanceTest",
            f"User {user_id}",
            role,
            status,
            format_dt(last_active_at),
            format_dt(created_at),
            format_dt(updated_at),
            format_dt(deleted_at),
        ]


def generate_wallets(count: int):
    for wallet_id in range(1, count + 1):
        user_id = wallet_id

        balance = random.randint(0, 100_000)
        event_balance = random.randint(0, 30_000)
        total_balance = balance + event_balance

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)

        yield [
            wallet_id,
            user_id,
            balance,
            event_balance,
            total_balance,
            format_dt(created_at),
            format_dt(updated_at),
        ]


def generate_point_histories(count: int, wallet_count: int):
    for history_id in range(1, count + 1):
        wallet_id = random.randint(1, wallet_count)
        point_type = weighted_choice([
            ("PAID", 70),
            ("FREE", 30),
        ])
        point_history_type = weighted_choice([
            ("CHARGE", 50),
            ("USE", 40),
            ("REFUND", 10),
        ])

        amount = random.randint(100, 50_000)
        balance_after = random.randint(0, 200_000)
        created_at = random_datetime_between(BASE_DATE, NOW)

        yield [
            history_id,
            wallet_id,
            point_type,
            point_history_type,
            amount,
            balance_after,
            f"Seed point history {history_id}",
            format_dt(created_at),
        ]


def generate_artists(count: int, user_count: int):
    for artist_id in range(1, count + 1):
        owner_user_id = random.randint(1, user_count)
        artist_type = weighted_choice([
            ("SOLO", 90),
            ("COLLABORATION", 10),
        ])
        status = weighted_choice([
            ("ACTIVE", 90),
            ("INACTIVE", 10),
        ])

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        deleted_at = random_datetime_between(updated_at, NOW) if random.random() < 0.05 else None

        yield [
            artist_id,
            owner_user_id,
            f"Artist {artist_id}",
            artist_type,
            f"Bio for artist {artist_id}",
            f"https://cdn.fivefy.local/artists/{artist_id}.jpg",
            status,
            format_dt(created_at),
            format_dt(updated_at),
            format_dt(deleted_at),
        ]


def generate_albums(count: int, artist_count: int):
    for album_id in range(1, count + 1):
        artist_id = random.randint(1, artist_count)
        status = weighted_choice([
            ("PUBLISHED", 70),
            ("UNPUBLISHED", 20),
            ("BLOCKED", 10),
        ])

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        published_at = random_datetime_between(created_at, NOW) if status == "PUBLISHED" else None
        scheduled_publish_at = None if status == "PUBLISHED" else created_at + timedelta(days=random.randint(1, 7))
        deleted_at = random_datetime_between(updated_at, NOW) if random.random() < 0.05 else None

        yield [
            album_id,
            artist_id,
            f"Album {album_id}",
            f"Description for album {album_id}",
            f"https://cdn.fivefy.local/albums/{album_id}.jpg",
            status,
            format_dt(scheduled_publish_at),
            format_dt(published_at),
            0,
            0,
            format_dt(created_at),
            format_dt(updated_at),
            format_dt(deleted_at),
        ]


def generate_tracks(count: int, user_count: int, artist_count: int, album_count: int):
    for track_id in range(1, count + 1):
        track_type = weighted_choice([
            ("OFFICIAL_RELEASE", 70),
            ("FREE_CREATION", 30),
        ])
        status = weighted_choice([
            ("PUBLISHED", 70),
            ("UNPUBLISHED", 20),
            ("BLOCKED", 10),
        ])

        owner_user_id = random.randint(1, user_count)

        if track_type == "OFFICIAL_RELEASE":
            artist_id = random.randint(1, artist_count)
            album_id = random.randint(1, album_count)
            track_number = random.randint(1, 20)
            featured_artist_text = None if random.random() < 0.8 else f"Featured Artist {random.randint(1, 1000)}"
        else:
            artist_id = None
            album_id = None
            track_number = None
            featured_artist_text = None

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        published_at = random_datetime_between(created_at, NOW) if status == "PUBLISHED" else None
        scheduled_publish_at = None if status == "PUBLISHED" else created_at + timedelta(days=random.randint(1, 7))
        deleted_at = random_datetime_between(updated_at, NOW) if random.random() < 0.05 else None

        play_count = int(random.paretovariate(1.5) * 100)
        duration_sec = random.randint(60, 420)

        yield [
            track_id,
            owner_user_id,
            track_type,
            nullable(artist_id),
            nullable(album_id),
            nullable(track_number),
            f"Track {track_id}",
            f"Lyrics for track {track_id}",
            f"GENRE_{random.randint(1, 20)}",
            f"https://cdn.fivefy.local/audio/{track_id}.mp3",
            duration_sec,
            nullable(featured_artist_text),
            status,
            format_dt(scheduled_publish_at),
            format_dt(published_at),
            play_count,
            format_dt(created_at),
            format_dt(updated_at),
            format_dt(deleted_at),
        ]


def generate_track_comments(count: int, user_count: int, track_count: int):
    hot_track_count = min(100, track_count)
    hot_comment_limit = count // 2

    for comment_id in range(1, count + 1):
        user_id = random.randint(1, user_count)

        if comment_id <= hot_comment_limit:
            track_id = random.randint(1, hot_track_count)
        else:
            track_id = random.randint(1, track_count)

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)
        deleted_at = random_datetime_between(updated_at, NOW) if random.random() < 0.10 else None

        yield [
            comment_id,
            user_id,
            track_id,
            f"Track comment content {comment_id}",
            format_dt(created_at),
            format_dt(updated_at),
            format_dt(deleted_at),
        ]


def generate_artist_applications(count: int, user_count: int):
    for application_id in range(1, count + 1):
        requester_user_id = random.randint(1, user_count)
        status = weighted_choice([
            ("PENDING", 30),
            ("APPROVED", 40),
            ("REJECTED", 30),
        ])

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)

        reviewed_by_admin_id = None
        reviewed_at = None
        rejection_reason = None

        if status in ("APPROVED", "REJECTED"):
            reviewed_by_admin_id = random.randint(1, max(1, user_count // 200))
            reviewed_at = random_datetime_between(created_at, NOW)

        if status == "REJECTED":
            rejection_reason = "등록 신청 기준에 부합하지 않습니다."

        yield [
            application_id,
            requester_user_id,
            f"Requested Artist {application_id}",
            weighted_choice([("SOLO", 90), ("COLLABORATION", 10)]),
            f"Requested artist bio {application_id}",
            f"https://cdn.fivefy.local/artist-applications/{application_id}.jpg",
            status,
            nullable(reviewed_by_admin_id),
            format_dt(reviewed_at),
            nullable(rejection_reason),
            format_dt(created_at),
            format_dt(updated_at),
        ]


def generate_album_applications(count: int, user_count: int, artist_count: int):
    for application_id in range(1, count + 1):
        requester_user_id = random.randint(1, user_count)
        artist_id = random.randint(1, artist_count)
        status = weighted_choice([
            ("PENDING", 30),
            ("APPROVED", 40),
            ("REJECTED", 30),
        ])

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)

        reviewed_by_admin_id = None
        reviewed_at = None
        rejection_reason = None

        if status in ("APPROVED", "REJECTED"):
            reviewed_by_admin_id = random.randint(1, max(1, user_count // 200))
            reviewed_at = random_datetime_between(created_at, NOW)

        if status == "REJECTED":
            rejection_reason = "앨범 등록 신청 기준에 부합하지 않습니다."

        yield [
            application_id,
            requester_user_id,
            artist_id,
            f"Requested Album {application_id}",
            f"Requested album description {application_id}",
            f"https://cdn.fivefy.local/album-applications/{application_id}.jpg",
            random.randint(0, 7),
            status,
            nullable(reviewed_by_admin_id),
            format_dt(reviewed_at),
            nullable(rejection_reason),
            format_dt(created_at),
            format_dt(updated_at),
        ]


def generate_track_applications(count: int, user_count: int, artist_count: int, album_count: int):
    for application_id in range(1, count + 1):
        requester_user_id = random.randint(1, user_count)
        track_type = weighted_choice([
            ("OFFICIAL_RELEASE", 70),
            ("FREE_CREATION", 30),
        ])
        status = weighted_choice([
            ("PENDING", 30),
            ("APPROVED", 40),
            ("REJECTED", 30),
        ])

        if track_type == "OFFICIAL_RELEASE":
            artist_id = random.randint(1, artist_count)
            album_id = random.randint(1, album_count)
            track_number = random.randint(1, 20)
            featured_artist_text = None if random.random() < 0.8 else f"Featured Artist {random.randint(1, 1000)}"
            publish_delay_days = random.randint(0, 7)
        else:
            artist_id = None
            album_id = None
            track_number = None
            featured_artist_text = None
            publish_delay_days = None

        created_at = random_datetime_between(BASE_DATE, NOW)
        updated_at = random_datetime_between(created_at, NOW)

        reviewed_by_admin_id = None
        reviewed_at = None
        rejection_reason = None

        if status in ("APPROVED", "REJECTED"):
            reviewed_by_admin_id = random.randint(1, max(1, user_count // 200))
            reviewed_at = random_datetime_between(created_at, NOW)

        if status == "REJECTED":
            rejection_reason = "트랙 등록 신청 기준에 부합하지 않습니다."

        yield [
            application_id,
            requester_user_id,
            track_type,
            nullable(artist_id),
            nullable(album_id),
            nullable(track_number),
            f"Requested Track {application_id}",
            f"Requested lyrics {application_id}",
            f"GENRE_{random.randint(1, 20)}",
            f"https://cdn.fivefy.local/application-audio/{application_id}.mp3",
            random.randint(60, 420),
            nullable(featured_artist_text),
            nullable(publish_delay_days),
            status,
            nullable(reviewed_by_admin_id),
            format_dt(reviewed_at),
            nullable(rejection_reason),
            format_dt(created_at),
            format_dt(updated_at),
        ]


def generate_all(config: ScaleConfig, output_dir: Path) -> None:
    ensure_output_dir(output_dir)

    write_csv(
        output_dir / "users.csv",
        [
            "id",
            "email",
            "password",
            "name",
            "role",
            "status",
            "last_active_at",
            "created_at",
            "updated_at",
            "deleted_at",
        ],
        generate_users(config.users),
        )

    write_csv(
        output_dir / "wallets.csv",
        [
            "id",
            "user_id",
            "balance",
            "event_balance",
            "total_balance",
            "created_at",
            "updated_at",
        ],
        generate_wallets(config.wallets),
        )

    write_csv(
        output_dir / "point_histories.csv",
        [
            "id",
            "wallet_id",
            "point_type",
            "point_history_type",
            "amount",
            "balance_after",
            "log_description",
            "created_at",
        ],
        generate_point_histories(config.point_histories, config.wallets),
        )

    write_csv(
        output_dir / "artists.csv",
        [
            "id",
            "owner_user_id",
            "name",
            "artist_type",
            "bio",
            "profile_image_url",
            "status",
            "created_at",
            "updated_at",
            "deleted_at",
        ],
        generate_artists(config.artists, config.users),
        )

    write_csv(
        output_dir / "albums.csv",
        [
            "id",
            "artist_id",
            "title",
            "description",
            "cover_image_url",
            "status",
            "scheduled_publish_at",
            "published_at",
            "track_count",
            "total_duration_sec",
            "created_at",
            "updated_at",
            "deleted_at",
        ],
        generate_albums(config.albums, config.artists),
        )

    write_csv(
        output_dir / "tracks.csv",
        [
            "id",
            "owner_user_id",
            "track_type",
            "artist_id",
            "album_id",
            "track_number",
            "title",
            "lyrics",
            "genre",
            "audio_url",
            "duration_sec",
            "featured_artist_text",
            "status",
            "scheduled_publish_at",
            "published_at",
            "play_count",
            "created_at",
            "updated_at",
            "deleted_at",
        ],
        generate_tracks(config.tracks, config.users, config.artists, config.albums),
        )

    write_csv(
        output_dir / "track_comments.csv",
        [
            "id",
            "user_id",
            "track_id",
            "content",
            "created_at",
            "updated_at",
            "deleted_at",
        ],
        generate_track_comments(config.track_comments, config.users, config.tracks),
        )

    write_csv(
        output_dir / "artist_applications.csv",
        [
            "id",
            "requester_user_id",
            "requested_name",
            "artist_type",
            "bio",
            "profile_image_url",
            "status",
            "reviewed_by_admin_id",
            "reviewed_at",
            "rejection_reason",
            "created_at",
            "updated_at",
        ],
        generate_artist_applications(config.artist_applications, config.users),
        )

    write_csv(
        output_dir / "album_applications.csv",
        [
            "id",
            "requester_user_id",
            "artist_id",
            "title",
            "description",
            "cover_image_url",
            "publish_delay_days",
            "status",
            "reviewed_by_admin_id",
            "reviewed_at",
            "rejection_reason",
            "created_at",
            "updated_at",
        ],
        generate_album_applications(config.album_applications, config.users, config.artists),
        )

    write_csv(
        output_dir / "track_applications.csv",
        [
            "id",
            "requester_user_id",
            "track_type",
            "artist_id",
            "album_id",
            "track_number",
            "title",
            "lyrics",
            "genre",
            "audio_url",
            "duration_sec",
            "featured_artist_text",
            "publish_delay_days",
            "status",
            "reviewed_by_admin_id",
            "reviewed_at",
            "rejection_reason",
            "created_at",
            "updated_at",
        ],
        generate_track_applications(
            config.track_applications,
            config.users,
            config.artists,
            config.albums,
        ),
        )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate integrated seed CSV files for performance testing."
    )
    parser.add_argument(
        "--scale",
        choices=SCALE_CONFIGS.keys(),
        default="smoke",
        help="Dataset scale.",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=42,
        help="Random seed for reproducible data generation.",
    )
    parser.add_argument(
        "--output-dir",
        type=str,
        default="scripts/seed/output",
        help="CSV output directory.",
    )

    return parser.parse_args()


def main() -> None:
    args = parse_args()
    random.seed(args.seed)

    output_dir = Path(args.output_dir) / args.scale
    config = SCALE_CONFIGS[args.scale]

    print(f"[seed] scale={args.scale}")
    print(f"[seed] output_dir={output_dir}")
    print(f"[seed] config={config}")

    generate_all(config, output_dir)

    print("[seed] CSV generation completed.")


if __name__ == "__main__":
    main()