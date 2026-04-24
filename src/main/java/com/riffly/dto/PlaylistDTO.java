package com.riffly.dto;

import com.riffly.model.Playlist;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class PlaylistDTO {

    @Getter @Setter
    public static class CreateRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String description;
    }

    @Getter @Setter
    public static class UpdateRequest {
        private String name;
        private String description;
    }

    @Getter @Setter
    public static class AddSongRequest {
        @NotNull(message = "songId is required")
        private Long songId;
    }

    @Getter @Builder
    public static class Response {
        private Long                  id;
        private String                name;
        private String                description;
        private String                ownerUsername;
        private int                   songCount;
        private List<SongDTO.Response> songs;
        private Instant               createdAt;
        private Instant               updatedAt;

        public static Response from(Playlist playlist) {
            List<SongDTO.Response> songs = playlist.getSongs().stream()
                    .map(SongDTO.Response::from)
                    .collect(Collectors.toList());
            return Response.builder()
                    .id(playlist.getId())
                    .name(playlist.getName())
                    .description(playlist.getDescription())
                    .ownerUsername(playlist.getOwner() != null
                            ? playlist.getOwner().getUsername() : null)
                    .songCount(songs.size())
                    .songs(songs)
                    .createdAt(playlist.getCreatedAt())
                    .updatedAt(playlist.getUpdatedAt())
                    .build();
        }
    }
}
