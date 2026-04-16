package com.fivefy.domain.album.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.dto.request.AlbumReleaseRequestCreateRequest;
import com.fivefy.domain.album.dto.response.AlbumReleaseRequestCreateResponse;
import com.fivefy.domain.album.entity.AlbumReleaseRequest;
import com.fivefy.domain.album.enums.AlbumReleaseErrorCode;
import com.fivefy.domain.album.repository.AlbumReleaseRequestRepository;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.enums.ArtistStatus;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 앨범 도메인 서비스
 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumReleaseRequestRepository albumReleaseRequestRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    /**
     * 앨범 등록 요청을 생성한다.
     *
     * 검증 흐름:
     * 1. 요청 유저 존재 여부 확인
     * 2. 삭제되지 않은 아티스트 조회
     * 3. 아티스트 소유자 검증
     * 4. 아티스트 상태 검증
     * 5. 공개 예약 옵션 검증
     * 6. 동일 조건의 진행 중 요청 중복 검증
     */
    @Transactional
    public AlbumReleaseRequestCreateResponse createAlbumReleaseRequest(
            Long userId,
            AlbumReleaseRequestCreateRequest request
    ) {
        // 존재하지 않는 유저 요청을 초기에 차단
        findUser(userId);

        // 삭제된 아티스트는 존재하지 않는 것처럼 처리
        Artist artist = findNotDeletedArtist(request.artistId());

        // 본인이 소유한 아티스트만 앨범 등록 요청 가능
        validateArtistOwner(userId, artist);

        // 비활성화된 아티스트는 앨범 등록 요청 불가
        validateArtistActive(artist);

        // 정책상 허용된 공개 옵션인지 검증
        validatePublishDelayDays(request.publishDelayDays());

        // 동일 조건(PENDING)의 중복 요청 방지
        validateDuplicatePendingRequest(userId, request.artistId(), request.title());

        AlbumReleaseRequest albumReleaseRequest = AlbumReleaseRequest.create(
                userId,
                request.artistId(),
                request.title(),
                request.description(),
                request.coverImageUrl(),
                request.publishDelayDays()
        );

        AlbumReleaseRequest saved = albumReleaseRequestRepository.save(albumReleaseRequest);

        return AlbumReleaseRequestCreateResponse.from(saved);
    }

    // 유저 존재 여부를 검증
    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    // 아티스트 조회
    private Artist findArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND));
    }

    /**
     * 삭제되지 않은 아티스트 조회
     *
     * 삭제된 아티스트는 외부에서 접근할 수 없도록 NOT_FOUND 처리
     */
    private Artist findNotDeletedArtist(Long artistId) {
        Artist artist = findArtist(artistId);

        if (artist.isDeleted()) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND);
        }

        return artist;
    }

    // 아티스트 소유자 검증
    private void validateArtistOwner(Long userId, Artist artist) {
        if (!artist.isOwnedBy(userId)) {
            throw new BusinessException(ArtistErrorCode.ERR_FORBIDDEN_ARTIST_ACCESS);
        }
    }

    // 앨범 등록 요청 가능한 아티스트 상태 검증
    private void validateArtistActive(Artist artist) {
        if (artist.getStatus() != ArtistStatus.ACTIVE) {
            throw new BusinessException(
                    AlbumReleaseErrorCode.ERR_INACTIVE_ARTIST_CANNOT_REQUEST_ALBUM_RELEASE
            );
        }
    }

    /**
     * 공개 예약 옵션 검증
     *
     * 정책:
     * 0 = 즉시 공개
     * 1~7 = 승인 시점 기준 N일 후 공개
     */
    private void validatePublishDelayDays(Integer publishDelayDays) {
        if (publishDelayDays == null || publishDelayDays < 0 || publishDelayDays > 7) {
            throw new BusinessException(
                    AlbumReleaseErrorCode.ERR_INVALID_PUBLISH_DELAY_DAYS
            );
        }
    }

    // 동일 조건의 진행 중 요청(PENDING) 중복 방지
    private void validateDuplicatePendingRequest(Long userId, Long artistId, String title) {
        boolean exists = albumReleaseRequestRepository.existsPendingRequest(
                userId,
                artistId,
                title
        );

        if (exists) {
            throw new BusinessException(
                    AlbumReleaseErrorCode.ERR_ALBUM_RELEASE_ALREADY_EXISTS
            );
        }
    }
}