package com.fivefy.domain.album.repository;

/**
 * AlbumReleaseRequest Querydsl 전용 Repository
 */
public interface AlbumReleaseRequestQueryRepository {

    // 동일 유저, 동일 아티스트, 동일 제목으로 진행 중(PENDING) 요청 존재 여부
    boolean existsPendingRequest(Long requesterUserId, Long artistId, String title);
}