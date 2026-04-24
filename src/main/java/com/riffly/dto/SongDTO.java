package com.riffly.dto;

import com.riffly.model.Song;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;

public class SongDTO {

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Artist is required")
        private String artist;

        private String  album;
        private String  genre;

        @NotBlank(message = "File path is required")
        private String filePath;

        private Integer durationSeconds;
        private Long    fileSizeBytes;
        private String  mimeType;
    }

    @Getter @Setter
    public static class UpdateRequest {
        private String  title;
        private String  artist;
        private String  album;
        private String  genre;
        private Integer durationSeconds;
        private Long    fileSizeBytes;
        private String  mimeType;
    }

    @Getter @Builder
    public static class Response {
        private Long    id;
        private String  title;
        private String  artist;
        private String  album;
        private String  genre;
        private String  filePath;
        private Integer durationSeconds;
        private Long    fileSizeBytes;
        private String  mimeType;
        private Long    playCount;
        private Instant createdAt;
        private Instant updatedAt;
        private String  streamUrl;

        public static Response from(Song song) {
            return Response.builder()
                    .id(song.getId())
                    .title(song.getTitle())
                    .artist(song.getArtist())
                    .album(song.getAlbum())
                    .genre(song.getGenre())
                    .filePath(song.getFilePath())
                    .durationSeconds(song.getDurationSeconds())
                    .fileSizeBytes(song.getFileSizeBytes())
                    .mimeType(song.getMimeType())
                    .playCount(song.getPlayCount())
                    .createdAt(song.getCreatedAt())
                    .updatedAt(song.getUpdatedAt())
                    .streamUrl("/api/songs/" + song.getId() + "/stream")
                    .build();
        }
    }
}
