package com.fivefy.domain.album.repository;

import com.fivefy.common.enums.ApplicationStatus;
import com.fivefy.domain.album.entity.AlbumApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * AlbumApplication Querydsl 전용 Repository
 */
public interface AlbumApplicationQueryRepository {

    boolean existsPendingApplication(Long requesterUserId, Long artistId, String title);

    List<AlbumApplication> searchMyAlbumApplications(Long requesterUserId);

    Page<AlbumApplication> searchAlbumApplications(
            ApplicationStatus status,
            Pageable pageable
    );
}