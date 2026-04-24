-- ============================================================
-- Riffly v2 — Database Schema
-- Run once: psql -U riffly_user -d riffly_db -f init.sql
-- Hibernate ddl-auto=update will handle columns going forward.
-- ============================================================

-- Users
CREATE TABLE IF NOT EXISTS users (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at   TIMESTAMPTZ  DEFAULT NOW()
);

-- Songs
CREATE TABLE IF NOT EXISTS songs (
    id               BIGSERIAL    PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    artist           VARCHAR(255) NOT NULL DEFAULT 'Unknown Artist',
    album            VARCHAR(255),
    genre            VARCHAR(100),
    file_path        VARCHAR(500) NOT NULL UNIQUE,
    duration_seconds INTEGER,
    file_size_bytes  BIGINT,
    mime_type        VARCHAR(50)  DEFAULT 'audio/mpeg',
    play_count       BIGINT       DEFAULT 0,
    created_at       TIMESTAMPTZ  DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  DEFAULT NOW()
);

-- Playlists (owner_id nullable so existing seed data survives)
CREATE TABLE IF NOT EXISTS playlists (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW()
);

-- Playlist ↔ Song join table
CREATE TABLE IF NOT EXISTS playlist_songs (
    playlist_id BIGINT REFERENCES playlists(id) ON DELETE CASCADE,
    song_id     BIGINT REFERENCES songs(id)     ON DELETE CASCADE,
    PRIMARY KEY (playlist_id, song_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_songs_artist   ON songs(LOWER(artist));
CREATE INDEX IF NOT EXISTS idx_songs_title    ON songs(LOWER(title));
CREATE INDEX IF NOT EXISTS idx_songs_genre    ON songs(LOWER(genre));
CREATE INDEX IF NOT EXISTS idx_playlist_owner ON playlists(owner_id);
