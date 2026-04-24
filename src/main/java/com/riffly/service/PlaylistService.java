package com.riffly.service;

import com.riffly.dto.PlaylistDTO;
import com.riffly.exception.RifflyException;
import com.riffly.model.Playlist;
import com.riffly.model.Song;
import com.riffly.model.User;
import com.riffly.repository.PlaylistRepository;
import com.riffly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository     userRepository;
    private final SongService        songService;

    // ── Queries ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PlaylistDTO.Response> getAllForUser(String username) {
        User user = findUserOrThrow(username);
        return playlistRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(PlaylistDTO.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlaylistDTO.Response getById(Long id, String username) {
        Playlist playlist = findOrThrow(id);
        assertOwner(playlist, username);
        return PlaylistDTO.Response.from(playlist);
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    public PlaylistDTO.Response create(PlaylistDTO.CreateRequest req, String username) {
        User user = findUserOrThrow(username);
        Playlist playlist = Playlist.builder()
                .name(req.getName())
                .description(req.getDescription())
                .owner(user)
                .build();
        return PlaylistDTO.Response.from(playlistRepository.save(playlist));
    }

    public PlaylistDTO.Response update(Long id, PlaylistDTO.UpdateRequest req, String username) {
        Playlist playlist = findOrThrow(id);
        assertOwner(playlist, username);
        if (req.getName()        != null) playlist.setName(req.getName());
        if (req.getDescription() != null) playlist.setDescription(req.getDescription());
        return PlaylistDTO.Response.from(playlistRepository.save(playlist));
    }

    public void delete(Long id, String username) {
        Playlist playlist = findOrThrow(id);
        assertOwner(playlist, username);
        playlistRepository.deleteById(id);
    }

    public PlaylistDTO.Response addSong(Long playlistId, Long songId, String username) {
        Playlist playlist = findOrThrow(playlistId);
        assertOwner(playlist, username);
        Song song = songService.findOrThrow(songId);

        boolean alreadyAdded = playlist.getSongs().stream()
                .anyMatch(s -> s.getId().equals(songId));
        if (alreadyAdded) {
            throw RifflyException.conflict("Song already exists in this playlist");
        }

        playlist.getSongs().add(song);
        return PlaylistDTO.Response.from(playlistRepository.save(playlist));
    }

    public PlaylistDTO.Response removeSong(Long playlistId, Long songId, String username) {
        Playlist playlist = findOrThrow(playlistId);
        assertOwner(playlist, username);
        playlist.getSongs().removeIf(s -> s.getId().equals(songId));
        return PlaylistDTO.Response.from(playlistRepository.save(playlist));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Playlist findOrThrow(Long id) {
        return playlistRepository.findById(id)
                .orElseThrow(() -> RifflyException.notFound("Playlist", id));
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> RifflyException.notFound("User", -1L));
    }

    private void assertOwner(Playlist playlist, String username) {
        if (playlist.getOwner() == null) return; // legacy playlists without owner
        if (!playlist.getOwner().getUsername().equals(username)) {
            throw RifflyException.badRequest("You do not own this playlist");
        }
    }
}
