package com.fivefy.domain.search.repository;

import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.track.entity.Track;

import java.util.List;

public interface SearchQueryRepository {
    List<Artist> searchArtists(String keyword);

    List<Track> searchTracks(String keyword);

    List<Album> searchAlbums(String keyword);
}