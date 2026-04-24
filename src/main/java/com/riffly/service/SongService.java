package com.riffly.service;

import com.riffly.dto.SongDTO;
import com.riffly.exception.RifflyException;
import com.riffly.model.Song;
import com.riffly.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SongService {

    private final SongRepository songRepository;

    @Transactional(readOnly = true)
    public List<SongDTO.Response> getAll() {
        return songRepository.findAll().stream()
                .map(SongDTO.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SongDTO.Response getById(Long id) {
        return SongDTO.Response.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<SongDTO.Response> search(String query) {
        return songRepository.search(query).stream()
                .map(SongDTO.Response::from)
                .collect(Collectors.toList());
    }

    public SongDTO.Response create(SongDTO.CreateRequest req) {
        if (songRepository.existsByFilePath(req.getFilePath())) {
            throw RifflyException.conflict("A song with this file path already exists");
        }
        Song song = Song.builder()
                .title(req.getTitle())
                .artist(req.getArtist())
                .album(req.getAlbum())
                .genre(req.getGenre())
                .filePath(req.getFilePath())
                .durationSeconds(req.getDurationSeconds())
                .fileSizeBytes(req.getFileSizeBytes())
                .mimeType(req.getMimeType() != null ? req.getMimeType() : "audio/mpeg")
                .build();
        return SongDTO.Response.from(songRepository.save(song));
    }

    public SongDTO.Response update(Long id, SongDTO.UpdateRequest req) {
        Song song = findOrThrow(id);
        if (req.getTitle()           != null) song.setTitle(req.getTitle());
        if (req.getArtist()          != null) song.setArtist(req.getArtist());
        if (req.getAlbum()           != null) song.setAlbum(req.getAlbum());
        if (req.getGenre()           != null) song.setGenre(req.getGenre());
        if (req.getDurationSeconds() != null) song.setDurationSeconds(req.getDurationSeconds());
        if (req.getFileSizeBytes()   != null) song.setFileSizeBytes(req.getFileSizeBytes());
        if (req.getMimeType()        != null) song.setMimeType(req.getMimeType());
        return SongDTO.Response.from(songRepository.save(song));
    }

    public void delete(Long id) {
        findOrThrow(id);
        songRepository.deleteById(id);
    }

    public Song findOrThrow(Long id) {
        return songRepository.findById(id)
                .orElseThrow(() -> RifflyException.notFound("Song", id));
    }

    public void incrementPlayCount(Long id) {
        Song song = findOrThrow(id);
        song.setPlayCount(song.getPlayCount() + 1);
        songRepository.save(song);
    }
}
