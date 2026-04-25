# 🎵 Riffly

> A self-hosted music streaming platform built with Spring Boot and Vanilla JS.  
> Stream your local music library from any browser — with playlists, JWT auth, and auto metadata sync.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)

---

## Screenshots

<table>
  <tr>
    <td align="center"><b>Login Page</b></td>
    <td align="center"><b>Music Player</b></td>
  </tr>
  <tr>
    <td><img src="docs/screenshot-login.png" alt="Riffly Login" width="480"/></td>
    <td><img src="docs/screenshot-player.png" alt="Riffly Player" width="480"/></td>
  </tr>
</table>

---

## Features

- 🎧 **Stream local audio files** — MP3, FLAC, WAV, OGG, AAC, M4A
- 🔄 **Auto library sync** — drop files in a folder, restart, done
- 🏷️ **Metadata extraction** — reads ID3 tags via jaudiotagger; falls back to filename parsing
- 🔐 **JWT authentication** — stateless, secure, no sessions
- 📋 **User playlists** — create, manage, add/remove songs
- 🔍 **Search** — full-text across title, artist, album, genre
- ⏱️ **Duration display** — extracted directly from audio headers
- 🎨 **Modern UI** — dark glassmorphism design, animated background, 3D card tilt
- 📱 **Responsive** — works on desktop and tablet

---

## Tech Stack

**Backend**
- Java 17
- Spring Boot 3.2
- Spring Security + JWT (JJWT 0.12)
- Spring Data JPA + Hibernate
- PostgreSQL
- jaudiotagger (audio metadata)
- Lombok

**Frontend**
- Vanilla HTML / CSS / JavaScript
- No frameworks — zero dependencies
- Google Fonts (Syne + DM Sans)

---

## Project Structure

```
riffly/
├── frontend/
│   ├── login.html              # Auth page (register + login)
│   └── index.html              # Main app (player, playlists, search)
│
└── src/main/
    ├── resources/
    │   ├── application.properties
    │   └── init.sql             # DB schema
    │
    └── java/com/riffly/
        ├── config/
        │   ├── JwtProperties.java
        │   ├── SecurityConfig.java
        │   ├── StorageProperties.java
        │   └── WebConfig.java
        ├── controller/
        │   ├── AuthController.java
        │   ├── HealthController.java
        │   ├── PlaylistController.java
        │   └── SongController.java
        ├── dto/
        │   ├── ApiResponse.java
        │   ├── AuthDTO.java
        │   ├── PlaylistDTO.java
        │   └── SongDTO.java
        ├── exception/
        │   ├── GlobalExceptionHandler.java
        │   └── RifflyException.java
        ├── model/
        │   ├── Playlist.java
        │   ├── Song.java
        │   └── User.java
        ├── repository/
        │   ├── PlaylistRepository.java
        │   ├── SongRepository.java
        │   └── UserRepository.java
        ├── security/
        │   ├── JwtAuthenticationFilter.java
        │   ├── JwtService.java
        │   └── UserDetailsServiceImpl.java
        ├── service/
        │   ├── AuthService.java
        │   ├── PlaylistService.java
        │   ├── SongService.java
        │   ├── SongSyncService.java
        │   └── StreamingService.java
        └── RifflyApplication.java
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 1. Clone the repo

```bash
git clone https://github.com/yourusername/riffly.git
cd riffly
```

### 2. Set up PostgreSQL

```bash
psql postgres
```

```sql
CREATE USER riffly_user WITH PASSWORD 'riffly_pass';
CREATE DATABASE riffly_db OWNER riffly_user;
GRANT ALL PRIVILEGES ON DATABASE riffly_db TO riffly_user;
\q
```

Run the schema:

```bash
psql -U riffly_user -d riffly_db -f src/main/resources/init.sql
```

### 3. Configure

Edit `src/main/resources/application.properties`:

```properties
# Point this to your local music folder
riffly.storage.music-dir=/Users/yourname/music

# Change this secret in production
riffly.jwt.secret=your-random-secret-here
```

### 4. Add music

Drop `.mp3` / `.flac` / `.wav` files into your configured music directory.  
Files named `Artist - Title.mp3` will be parsed into proper metadata automatically.

### 5. Run

```bash
mvn spring-boot:run
```

On startup, you'll see the sync engine run:

```
[Sync] Discovered 24 audio file(s) on disk.
[Sync] + Added: 'Blinding Lights' by The Weeknd [214s]
[Sync] Sync complete — 24 added, 0 removed | Total: 24
```

### 6. Open the frontend

Serve `frontend/` with any static server. With VS Code Live Server:

1. Right-click `frontend/login.html` → **Open with Live Server**
2. Open `http://127.0.0.1:5500/login.html`
3. Register an account → start streaming

---

## API Reference

All endpoints are prefixed with `/api`.  
Protected routes require: `Authorization: Bearer <token>`

### Auth

| Method | Endpoint | Auth | Body |
|---|---|---|---|
| `POST` | `/auth/register` | ❌ | `{ username, password, displayName }` |
| `POST` | `/auth/login` | ❌ | `{ username, password }` |

### Songs

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/songs` | ❌ | List all songs |
| `GET` | `/songs/{id}` | ❌ | Get song by ID |
| `GET` | `/songs/search?query=` | ❌ | Search songs |
| `GET` | `/songs/{id}/stream` | ❌ | Stream audio (range requests supported) |
| `POST` | `/songs` | ✅ | Register a song manually |
| `PATCH` | `/songs/{id}` | ✅ | Update song metadata |
| `DELETE` | `/songs/{id}` | ✅ | Delete song |

### Playlists

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/playlists` | ✅ | Get your playlists |
| `GET` | `/playlists/{id}` | ✅ | Get playlist with songs |
| `POST` | `/playlists` | ✅ | Create playlist |
| `PATCH` | `/playlists/{id}` | ✅ | Rename / update playlist |
| `DELETE` | `/playlists/{id}` | ✅ | Delete playlist |
| `POST` | `/playlists/{id}/songs` | ✅ | Add song `{ songId }` |
| `DELETE` | `/playlists/{id}/songs/{songId}` | ✅ | Remove song |

### Other

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Health check |

---

## How the Sync Engine Works

Every time the backend starts, `SongSyncService` runs automatically:

1. **Scans** the configured music directory recursively
2. **Extracts metadata** from each file using jaudiotagger (ID3 / Vorbis / FLAC tags)
3. **Falls back** to filename parsing if tags are missing — `Artist - Title.mp3`
4. **Compares** each file against the database:
   - File not in DB → **INSERT**
   - File in DB but metadata changed → **UPDATE**
   - DB record with no file on disk → **DELETE**
5. **Logs** a summary of changes

The sync is fully **idempotent** — running it with no file changes results in zero DB writes.

---

## Environment Variables

For production, override `application.properties` via environment variables:

```bash
RIFFLY_STORAGE_MUSIC_DIR=/data/music
RIFFLY_JWT_SECRET=your-very-long-random-secret
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/riffly_db
SPRING_DATASOURCE_USERNAME=riffly_user
SPRING_DATASOURCE_PASSWORD=your_password
```

---

## Roadmap

- [ ] Upload songs via web UI
- [ ] Artist / album detail pages
- [ ] Shuffle and repeat modes
- [ ] Last.fm scrobbling
- [ ] Docker compose setup
- [ ] Mobile PWA support

---

## Contributing

Pull requests are welcome. For major changes, open an issue first.

1. Fork the repo
2. Create a branch: `git checkout -b feature/your-feature`
3. Commit: `git commit -m 'Add your feature'`
4. Push: `git push origin feature/your-feature`
5. Open a pull request

---

## License

[MIT](LICENSE) © 2024 Harshit Saini

---

<div align="center">
  <sub>Built with ☕ and Spring Boot</sub>
</div>
