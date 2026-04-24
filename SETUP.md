# 🎵 Riffly v2 — Setup Guide

## Prerequisites

Make sure these are installed on your Mac:

| Tool | Check | Install |
|---|---|---|
| Java 17+ | `java -version` | [adoptium.net](https://adoptium.net) |
| Maven | `mvn -version` | `brew install maven` |
| PostgreSQL | `psql --version` | `brew install postgresql` |
| Git | `git --version` | pre-installed on Mac |

---

## Step 1 — PostgreSQL Setup

Start PostgreSQL:
```bash
brew services start postgresql
```

Create database and user:
```bash
psql postgres
```

Inside psql, run:
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

---

## Step 2 — Music Folder

Create your music directory:
```bash
mkdir -p /Users/saini/riffly/music
```

Drop your `.mp3`, `.flac`, `.wav`, `.ogg`, `.aac`, or `.m4a` files in there.

> **Tip:** Files named like `Artist - Title.mp3` will be parsed automatically into proper metadata.

---

## Step 3 — Configuration

Open `src/main/resources/application.properties` and verify:

```properties
# Your music folder (already set)
riffly.storage.music-dir=/Users/saini/riffly/music

# Your DB credentials
spring.datasource.url=jdbc:postgresql://localhost:5432/riffly_db
spring.datasource.username=riffly_user
spring.datasource.password=riffly_pass

# JWT secret (change this in production!)
riffly.jwt.secret=cmln5y9XjP7qOzD3tNw8mK2rVhAeGbFsLuQxYpMdWiJcTvZ0nBsKaEoR1gH4fU6
```

---

## Step 4 — Build & Run Backend

Inside the project folder (`riffly-full-build/`):

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

You should see this in the logs on startup:
```
[Sync] ─── Starting Riffly library sync ───────────────────────
[Sync] Discovered 12 audio file(s) on disk.
[Sync] + Added: 'Blinding Lights' by The Weeknd [214s]
[Sync] Sync complete — 12 added, 0 removed | Total: 12
```

Backend is running at: **http://localhost:8080/api**

---

## Step 5 — Frontend

Open your frontend folder:
```bash
cd riffly-full-build/frontend/
```

**Option A — VS Code Live Server (recommended)**
1. Open VS Code
2. Install the **Live Server** extension
3. Right-click `login.html` → **Open with Live Server**
4. It opens at `http://127.0.0.1:5500/login.html`

**Option B — Python simple server**
```bash
cd riffly-full-build/frontend
python3 -m http.server 5500
# Open http://127.0.0.1:5500/login.html
```

---

## Step 6 — First Login

1. Go to `http://127.0.0.1:5500/login.html`
2. Click **Create Account**
3. Register with any username + password (min 6 chars)
4. You'll be redirected to the main app automatically
5. Your songs will be listed — click any to play 🎶

---

## Adding New Songs

Just drop `.mp3` files into:
```
/Users/saini/riffly/music
```

Then **restart the backend** — the sync engine will pick them up automatically:
```bash
# Stop with Ctrl+C, then:
mvn spring-boot:run
```

---

## API Endpoints Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | ❌ | Create account |
| POST | `/api/auth/login` | ❌ | Login, get JWT |
| GET | `/api/songs` | ❌ | List all songs |
| GET | `/api/songs/search?query=` | ❌ | Search songs |
| GET | `/api/songs/{id}/stream` | ❌ | Stream audio |
| GET | `/api/playlists` | ✅ | Your playlists |
| POST | `/api/playlists` | ✅ | Create playlist |
| POST | `/api/playlists/{id}/songs` | ✅ | Add song |
| DELETE | `/api/playlists/{id}/songs/{songId}` | ✅ | Remove song |
| DELETE | `/api/playlists/{id}` | ✅ | Delete playlist |
| GET | `/api/health` | ❌ | Health check |

---

## Troubleshooting

**`Could not connect to backend` in frontend**
- Make sure backend is running on port 8080
- Check CORS: frontend must be at `http://127.0.0.1:5500` not `localhost:5500`

**`relation "songs" does not exist`**
- Run `init.sql` again: `psql -U riffly_user -d riffly_db -f src/main/resources/init.sql`

**Songs not showing after adding files**
- Restart the backend — sync runs only on startup
- Check the music dir path in `application.properties`

**`Password authentication failed for user "riffly_user"`**
- Re-run the PostgreSQL setup in Step 1

**Port 8080 already in use**
```bash
lsof -ti:8080 | xargs kill -9
```

---

## Folder Structure

```
riffly-full-build/
├── pom.xml
├── frontend/
│   ├── login.html          ← Auth page
│   └── index.html          ← Main app
└── src/main/
    ├── resources/
    │   ├── application.properties
    │   └── init.sql
    └── java/com/riffly/
        ├── config/         ← JWT, CORS, Security, Storage config
        ├── controller/     ← REST endpoints
        ├── dto/            ← Request/Response objects
        ├── exception/      ← Error handling
        ├── model/          ← JPA entities (User, Song, Playlist)
        ├── repository/     ← Database queries
        ├── security/       ← JWT filter + UserDetails
        └── service/        ← Business logic + Sync engine
```

---

*Riffly v2 — Built with Spring Boot 3.2, PostgreSQL, JWT, jaudiotagger*
