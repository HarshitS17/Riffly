package com.riffly.controller;

import com.riffly.dto.ApiResponse;
import com.riffly.dto.SongDTO;
import com.riffly.service.SongService;
import com.riffly.service.StreamingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService      songService;
    private final StreamingService streamingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SongDTO.Response>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(songService.getAll()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SongDTO.Response>>> search(
            @RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.ok(songService.search(query)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SongDTO.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(songService.getById(id)));
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<InputStreamResource> stream(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        return streamingService.stream(id, rangeHeader);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SongDTO.Response>> create(
            @Valid @RequestBody SongDTO.CreateRequest req) {
        SongDTO.Response created = songService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Song registered successfully", created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<SongDTO.Response>> update(
            @PathVariable Long id,
            @RequestBody SongDTO.UpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Song updated", songService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        songService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Song deleted", null));
    }
}
