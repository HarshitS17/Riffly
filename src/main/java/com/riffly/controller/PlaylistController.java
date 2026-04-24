package com.riffly.controller;

import com.riffly.dto.ApiResponse;
import com.riffly.dto.PlaylistDTO;
import com.riffly.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    // ── Queries ───────────────────────────────────────────────────────────

    /** Returns only the playlists that belong to the authenticated user. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaylistDTO.Response>>> getAll(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                playlistService.getAllForUser(principal.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistDTO.Response>> getById(
            @PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                playlistService.getById(id, principal.getName())));
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistDTO.Response>> create(
            @Valid @RequestBody PlaylistDTO.CreateRequest req, Principal principal) {
        PlaylistDTO.Response created = playlistService.create(req, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Playlist created", created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<PlaylistDTO.Response>> update(
            @PathVariable Long id,
            @RequestBody PlaylistDTO.UpdateRequest req,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Playlist updated",
                playlistService.update(id, req, principal.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, Principal principal) {
        playlistService.delete(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok("Playlist deleted", null));
    }

    // ── Song membership ───────────────────────────────────────────────────

    @PostMapping("/{id}/songs")
    public ResponseEntity<ApiResponse<PlaylistDTO.Response>> addSong(
            @PathVariable Long id,
            @Valid @RequestBody PlaylistDTO.AddSongRequest req,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Song added to playlist",
                playlistService.addSong(id, req.getSongId(), principal.getName())));
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<ApiResponse<PlaylistDTO.Response>> removeSong(
            @PathVariable Long id,
            @PathVariable Long songId,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Song removed from playlist",
                playlistService.removeSong(id, songId, principal.getName())));
    }
}
