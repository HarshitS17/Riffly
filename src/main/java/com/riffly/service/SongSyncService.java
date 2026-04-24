package com.riffly.service;

import com.riffly.config.StorageProperties;
import com.riffly.model.Song;
import com.riffly.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════════════════
 *  SongSyncService  —  Production-level music library sync
 * ══════════════════════════════════════════════════════════════════════
 *
 *  Runs once every application startup via ApplicationRunner.
 *
 *  Algorithm
 *  ─────────
 *  1. Walk music directory (recursive) and collect all supported audio files.
 *  2. Load existing DB records keyed by absolute filePath.
 *  3. For each disk file:
 *       • Extract metadata via jaudiotagger (ID3/Vorbis/FLAC tags).
 *       • If NOT in DB → INSERT.
 *       • If already in DB → compare fields; UPDATE only if something changed.
 *  4. For each DB record with no matching disk file → DELETE (stale record).
 *  5. Log a concise summary: added / updated / removed / unchanged.
 *
 *  Metadata extraction strategy
 *  ─────────────────────────────
 *  1. Try ID3/Vorbis tags (jaudiotagger).
 *  2. If tag field is blank, try parsing filename: "Artist - Title.mp3".
 *  3. If still blank, apply safe defaults.
 *
 *  Idempotency
 *  ───────────
 *  Re-running with zero file changes = zero DB writes.
 *
 * ══════════════════════════════════════════════════════════════════════
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongSyncService implements ApplicationRunner {

    private final SongRepository    songRepository;
    private final StorageProperties storageProperties;

    // Silence jaudiotagger's own verbose logging — it logs at WARNING level for
    // non-fatal read issues and pollutes startup logs.
    static {
        Logger.getLogger("org.jaudiotagger").setLevel(Level.SEVERE);
    }

    private static final Map<String, String> MIME_MAP = Map.of(
            "mp3",  "audio/mpeg",
            "flac", "audio/flac",
            "wav",  "audio/wav",
            "ogg",  "audio/ogg",
            "aac",  "audio/aac",
            "m4a",  "audio/mp4"
    );

    // ══════════════════════════════════════════════════════════════════════
    //  Entry point
    // ══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Path musicDir = Path.of(storageProperties.getMusicDir());

        if (!Files.isDirectory(musicDir)) {
            log.warn("[Sync] Music directory not found: {}. Skipping sync.", musicDir);
            return;
        }

        log.info("[Sync] ─── Starting Riffly library sync ───────────────────────");
        log.info("[Sync] Directory: {}", musicDir.toAbsolutePath());

        Set<String> allowedExt = new HashSet<>(storageProperties.getAllowedExtensions());

        // ── 1. Scan disk ──────────────────────────────────────────────────
        List<Path> diskFiles = scanDirectory(musicDir, allowedExt);
        log.info("[Sync] Discovered {} audio file(s) on disk.", diskFiles.size());

        // ── 2. Load DB snapshot keyed by filePath ─────────────────────────
        Map<String, Song> dbByPath = songRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Song::getFilePath, s -> s));

        // ── 3. Process disk files ─────────────────────────────────────────
        int added = 0, updated = 0, unchanged = 0;
        Set<String> diskPaths = new HashSet<>();

        for (Path file : diskFiles) {
            String absPath = file.toAbsolutePath().toString();
            diskPaths.add(absPath);

            AudioMetadata meta = extractMetadata(file);

            if (!dbByPath.containsKey(absPath)) {
                // ── INSERT ──────────────────────────────────────────────
                Song song = buildSong(absPath, file, meta);
                songRepository.save(song);
                log.info("[Sync] + Added:   {}", describe(meta));
                added++;
            } else {
                // ── UPDATE if metadata changed ───────────────────────────
                Song existing = dbByPath.get(absPath);
                if (hasChanged(existing, meta)) {
                    applyMetadata(existing, meta);
                    songRepository.save(existing);
                    log.info("[Sync] ~ Updated: {}", describe(meta));
                    updated++;
                } else {
                    unchanged++;
                }
            }
        }

        // ── 4. Remove stale DB records ─────────────────────────────────────
        int removed = 0;
        for (Map.Entry<String, Song> entry : dbByPath.entrySet()) {
            if (!diskPaths.contains(entry.getKey())) {
                songRepository.delete(entry.getValue());
                log.info("[Sync] - Removed: {} (file deleted from disk)", entry.getKey());
                removed++;
            }
        }

        // ── 5. Summary ─────────────────────────────────────────────────────
        log.info("[Sync] ─── Sync complete ──────────────────────────────────────");
        log.info("[Sync] Added: {} | Updated: {} | Removed: {} | Unchanged: {} | Total: {}",
                added, updated, removed, unchanged, songRepository.count());
        log.info("[Sync] ──────────────────────────────────────────────────────────");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Disk scanning
    // ══════════════════════════════════════════════════════════════════════

    private List<Path> scanDirectory(Path dir, Set<String> allowedExt) {
        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String ext = extension(file.getFileName().toString());
                    if (allowedExt.contains(ext)) {
                        result.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException ex) {
                    log.warn("[Sync] Cannot access file {}: {}", file, ex.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("[Sync] Failed to walk music directory: {}", e.getMessage());
        }
        result.sort(Comparator.comparing(Path::toString));
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Metadata extraction  (jaudiotagger + filename fallback)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Attempts to read ID3/Vorbis/FLAC tags from the file.
     * Falls back gracefully on any read error.
     */
    private AudioMetadata extractMetadata(Path file) {
        String filename   = file.getFileName().toString();
        String ext        = extension(filename);
        String stemName   = stripExtension(filename);

        // --- defaults from filename ---
        String defaultTitle  = stemName;
        String defaultArtist = "Unknown Artist";

        // Try "Artist - Title" filename convention
        if (stemName.contains(" - ")) {
            String[] parts = stemName.split(" - ", 2);
            defaultArtist = capitalize(parts[0].trim());
            defaultTitle  = capitalize(parts[1].trim());
        } else if (stemName.contains(" – ")) { // en-dash
            String[] parts = stemName.split(" – ", 2);
            defaultArtist = capitalize(parts[0].trim());
            defaultTitle  = capitalize(parts[1].trim());
        } else {
            defaultTitle = capitalize(stemName.replace('_', ' ').replace('-', ' ').trim());
        }

        // --- jaudiotagger read ---
        try {
            AudioFile af     = AudioFileIO.read(file.toFile());
            AudioHeader hdr  = af.getAudioHeader();
            Tag tag          = af.getTag();

            String title    = tagValue(tag, FieldKey.TITLE,  defaultTitle);
            String artist   = tagValue(tag, FieldKey.ARTIST, defaultArtist);
            String album    = tagValue(tag, FieldKey.ALBUM,  null);
            String genre    = tagValue(tag, FieldKey.GENRE,  null);
            int    duration = hdr != null ? hdr.getTrackLength() : 0;

            long fileSize = fileSize(file);
            String mime   = MIME_MAP.getOrDefault(ext, "audio/mpeg");

            return new AudioMetadata(title, artist, album, genre,
                    duration > 0 ? duration : null, fileSize, mime);

        } catch (Exception e) {
            // jaudiotagger can't parse some formats — use filename-derived values
            log.debug("[Sync] Tag read failed for '{}': {}. Using filename fallback.",
                    filename, e.getMessage());

            return new AudioMetadata(
                    defaultTitle, defaultArtist, null, null,
                    null, fileSize(file),
                    MIME_MAP.getOrDefault(ext, "audio/mpeg"));
        }
    }

    /** Reads a tag field; returns fallback if tag is null or field is blank. */
    private String tagValue(Tag tag, FieldKey key, String fallback) {
        if (tag == null) return fallback;
        try {
            String val = tag.getFirst(key);
            return (val != null && !val.isBlank()) ? val.trim() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  DB helpers
    // ══════════════════════════════════════════════════════════════════════

    private Song buildSong(String absPath, Path file, AudioMetadata meta) {
        return Song.builder()
                .filePath(absPath)
                .title(meta.title())
                .artist(meta.artist())
                .album(meta.album())
                .genre(meta.genre())
                .durationSeconds(meta.durationSeconds())
                .fileSizeBytes(meta.fileSizeBytes())
                .mimeType(meta.mimeType())
                .playCount(0L)
                .build();
    }

    /**
     * Returns true if any persisted field differs from extracted metadata.
     * This is what makes the sync idempotent with no unnecessary writes.
     */
    private boolean hasChanged(Song song, AudioMetadata meta) {
        return !Objects.equals(song.getTitle(),           meta.title())
            || !Objects.equals(song.getArtist(),          meta.artist())
            || !Objects.equals(song.getAlbum(),           meta.album())
            || !Objects.equals(song.getGenre(),           meta.genre())
            || !Objects.equals(song.getDurationSeconds(), meta.durationSeconds())
            || !Objects.equals(song.getFileSizeBytes(),   meta.fileSizeBytes())
            || !Objects.equals(song.getMimeType(),        meta.mimeType());
    }

    private void applyMetadata(Song song, AudioMetadata meta) {
        song.setTitle(meta.title());
        song.setArtist(meta.artist());
        song.setAlbum(meta.album());
        song.setGenre(meta.genre());
        song.setDurationSeconds(meta.durationSeconds());
        song.setFileSizeBytes(meta.fileSizeBytes());
        song.setMimeType(meta.mimeType());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Utility
    // ══════════════════════════════════════════════════════════════════════

    private long fileSize(Path file) {
        try { return Files.size(file); }
        catch (IOException e) { return 0L; }
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0 && dot < filename.length() - 1)
                ? filename.substring(dot + 1).toLowerCase()
                : "";
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Arrays.stream(s.split("\\s+"))
                .map(w -> w.isEmpty() ? w
                        : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private String describe(AudioMetadata m) {
        return String.format("'%s' by %s [%ss]",
                m.title(), m.artist(),
                m.durationSeconds() != null ? m.durationSeconds() : "?");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Internal record — immutable metadata bag
    // ══════════════════════════════════════════════════════════════════════

    private record AudioMetadata(
            String  title,
            String  artist,
            String  album,
            String  genre,
            Integer durationSeconds,
            long    fileSizeBytes,
            String  mimeType
    ) {}
}
