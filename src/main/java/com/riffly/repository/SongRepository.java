package com.riffly.repository;

import com.riffly.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    @Query("SELECT s FROM Song s WHERE " +
           "LOWER(s.title)  LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(s.artist) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(s.album)  LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(s.genre)  LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Song> search(@Param("q") String query);

    List<Song>      findByGenreIgnoreCase(String genre);
    List<Song>      findByArtistIgnoreCase(String artist);
    boolean         existsByFilePath(String filePath);
    Optional<Song>  findByFilePath(String filePath);

    /** Lightweight projection: only loads filePath strings, not full entities. */
    @Query("SELECT s.filePath FROM Song s")
    List<String> findAllFilePaths();

    @Modifying
    @Query("DELETE FROM Song s WHERE s.filePath = :filePath")
    int deleteByFilePath(@Param("filePath") String filePath);
}
