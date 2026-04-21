package com.fivefy.domain.album.repository;

import com.fivefy.domain.album.entity.Album;

import java.util.List;

/**
 * Album Querydsl 전용 Repository
 */
public interface AlbumQueryRepository {

    List<Album> searchArtistAlbums(Long artistId);
}