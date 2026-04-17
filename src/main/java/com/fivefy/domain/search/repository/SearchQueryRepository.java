package com.fivefy.domain.search.repository;

import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.track.entity.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SearchQueryRepository {
    List<Artist> searchArtists(String keyword, int limit);

    Page<Track> searchTracks(String keyword, List<Long> artistIds, Pageable pageable);

    Page<Album> searchAlbums(String keyword, List<Long> artistIds, Pageable pageable);
}
