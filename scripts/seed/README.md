# Integrated Seed - Playlist Update

## 반영 파일 위치

- `scripts/seed/datasets/integrated/generate.py`
- `scripts/seed/datasets/integrated/insert.py`
- `scripts/seed/datasets/integrated/clean.sql`
- `scripts/seed/datasets/integrated/validate.sql`
- `scripts/seed/README.md`

## 추가 대상

- `playlists.csv`
- `playlist_tracks.csv`

## 컬럼 기준

### playlists

```text
id,user_id,title,description,deleted,updated_at,deleted_at,created_at
```

### playlist_tracks

```text
id,playlist_id,track_id,position,created_at
```

## 로컬 커밋 정리

```bash
git add scripts/seed/datasets/integrated/generate.py \
        scripts/seed/datasets/integrated/insert.py \
        scripts/seed/datasets/integrated/clean.sql \
        scripts/seed/datasets/integrated/validate.sql \
        scripts/seed/README.md

git commit -m "feat: 플레이리스트 seed 데이터 추가"
```
