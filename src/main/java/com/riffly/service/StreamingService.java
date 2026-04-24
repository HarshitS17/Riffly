package com.riffly.service;

import com.riffly.config.StorageProperties;
import com.riffly.exception.RifflyException;
import com.riffly.model.Song;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final StorageProperties storageProperties;
    private final SongService       songService;

    private static final long DEFAULT_CHUNK = 524_288L; // 512 KB

    public ResponseEntity<InputStreamResource> stream(Long songId, String rangeHeader) {
        Song song = songService.findOrThrow(songId);

        // SongSyncService stores absolute paths — resolve them directly.
        Path filePath = resolveFilePath(song.getFilePath());

        if (!Files.exists(filePath)) {
            throw RifflyException.fileNotFound(song.getFilePath());
        }

        long fileSize;
        try {
            fileSize = Files.size(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file size: " + e.getMessage());
        }

        String mimeType = song.getMimeType() != null ? song.getMimeType() : "audio/mpeg";

        if (rangeHeader == null || rangeHeader.isEmpty()) {
            return serveFullFile(filePath, fileSize, mimeType, songId);
        }

        long[] range        = parseRange(rangeHeader, fileSize);
        long   start        = range[0];
        long   end          = range[1];
        long   contentLength = end - start + 1;

        try {
            InputStream inputStream = Files.newInputStream(filePath);
            inputStream.skip(start);
            InputStream bounded = new BoundedInputStream(inputStream, contentLength);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + start + "-" + end + "/" + fileSize);
            headers.setContentLength(contentLength);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");

            songService.incrementPlayCount(songId);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(new InputStreamResource(bounded));

        } catch (IOException e) {
            throw new RuntimeException("Streaming error: " + e.getMessage());
        }
    }

    private ResponseEntity<InputStreamResource> serveFullFile(
            Path filePath, long fileSize, String mimeType, Long songId) {
        try {
            InputStream inputStream = Files.newInputStream(filePath);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setContentLength(fileSize);
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");

            songService.incrementPlayCount(songId);

            return ResponseEntity.ok().headers(headers)
                    .body(new InputStreamResource(inputStream));

        } catch (IOException e) {
            throw new RuntimeException("File read error: " + e.getMessage());
        }
    }

    private long[] parseRange(String rangeHeader, long fileSize) {
        if (!rangeHeader.startsWith("bytes=")) {
            throw RifflyException.badRequest("Invalid Range header format");
        }
        String rangeSpec = rangeHeader.substring(6).trim();
        long start, end;

        if (rangeSpec.startsWith("-")) {
            long suffix = Long.parseLong(rangeSpec.substring(1));
            start = Math.max(0, fileSize - suffix);
            end   = fileSize - 1;
        } else {
            String[] parts = rangeSpec.split("-");
            start = Long.parseLong(parts[0]);
            end   = (parts.length == 1 || parts[1].isEmpty())
                    ? Math.min(start + DEFAULT_CHUNK - 1, fileSize - 1)
                    : Long.parseLong(parts[1]);
        }

        if (start < 0 || end >= fileSize || start > end) {
            throw RifflyException.badRequest(
                    "Range not satisfiable: " + rangeHeader + " (size: " + fileSize + ")");
        }
        return new long[]{start, end};
    }

    /**
     * Resolves the stored filePath.
     * SongSyncService stores absolute paths, but supports legacy relative paths too.
     */
    private Path resolveFilePath(String storedPath) {
        Path path = Paths.get(storedPath);

        // Absolute path (from SongSyncService) — use directly
        if (path.isAbsolute()) {
            return path.normalize();
        }

        // Legacy relative path — resolve against music dir with traversal check
        Path base     = Paths.get(storageProperties.getMusicDir()).toAbsolutePath().normalize();
        Path resolved = base.resolve(storedPath).normalize();
        if (!resolved.startsWith(base)) {
            throw RifflyException.badRequest("Invalid file path");
        }
        return resolved;
    }

    // ── Bounded InputStream ───────────────────────────────────────────────
    private static class BoundedInputStream extends FilterInputStream {
        private long remaining;

        BoundedInputStream(InputStream in, long limit) {
            super(in);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = super.read();
            if (b != -1) remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead    = (int) Math.min(len, remaining);
            int bytesRead = super.read(b, off, toRead);
            if (bytesRead != -1) remaining -= bytesRead;
            return bytesRead;
        }
    }
}
