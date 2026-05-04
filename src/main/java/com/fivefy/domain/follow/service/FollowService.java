package com.fivefy.domain.follow.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.dto.response.FollowGetResponse;
import com.fivefy.domain.follow.entity.Follow;
import com.fivefy.domain.follow.enums.FollowErrorCode;
import com.fivefy.domain.follow.repository.FollowRepository;
import com.fivefy.domain.notification.entity.NotificationOutbox;
import com.fivefy.domain.notification.enums.NotificationType;
import com.fivefy.domain.notification.repository.NotificationOutboxRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final NotificationOutboxRepository outboxRepository;

    // 팔로우 등록
    @Transactional
    public FollowCreateResponse createFollow(Long userId, Long artistId) {
        User user = getUser(userId);
        Artist artist = getArtist(artistId);
        // 중복 검증
        if (followRepository.existsByUserIdAndArtistId(userId, artistId)) {
            throw new BusinessException(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS);
        }

        try {
            Follow follow = Follow.create(artist.getId(), user.getId());
            followRepository.save(follow);

            outboxRepository.save(NotificationOutbox.create(
                    NotificationType.NEW_FOLLOWER,
                    artist.getOwnerUserId(),
                    user.getId(),
                    null,
                    user.getName() + "님이 팔로우했습니다"
            ));

            return FollowCreateResponse.from(follow);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS);
        }
    }

    // 팔로우 목록 조회
    @Transactional(readOnly = true)
    public Page<FollowGetResponse> getFollows(Long userId, Pageable pageable) {
        getUser(userId);

        return followRepository.findAllByUserId(userId, pageable)
                .map(FollowGetResponse::from);
    }

    // 팔로우 취소
    @Transactional
    public void deleteFollow(Long userId, Long artistId) {
        Follow follow = getFollow(userId, artistId);

        followRepository.delete(follow);
    }

    // 알림설정
    @Transactional
    public void toggleNotification(Long userId, Long artistId) {
        Follow follow = getFollow(userId, artistId);

        follow.toggleNotification();
    }

    //중복되는 검증로직 헬퍼메서드
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    private Artist getArtist(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND));
    }

    // 권한 체크
    private Follow getFollow(Long userId, Long artistId) {
        return followRepository.findByUserIdAndArtistId(userId, artistId)
                .orElseThrow(() -> new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND));
    }
}
