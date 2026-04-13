package com.fivefy.domain.follow.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.dto.response.FollowGetResponse;
import com.fivefy.domain.follow.entity.Follow;
import com.fivefy.domain.follow.enums.FollowErrorCode;
import com.fivefy.domain.follow.repository.FollowRepository;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    // 팔로우 등록
    @Transactional
    public FollowCreateResponse createFollow(Long userId, Long artistId) {
        validateUserId(userId);
        if (!artistRepository.existsById(artistId)) {
            throw new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND);
        }
        // 중복검증
        if (followRepository.existsByUserIdAndArtistId(userId, artistId)) {
            throw new BusinessException(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS);
        }

        Follow follow = Follow.create(artistId, userId);
        followRepository.save(follow);

        return FollowCreateResponse.from(follow);
    }

    // 팔로우 목록 조회
    @Transactional(readOnly = true)
    public List<FollowGetResponse> getFollows(Long userId) {
        validateUserId(userId);

        return followRepository.findAllByUserId(userId)
                .stream()
                .map(FollowGetResponse::from)
                .toList();
    }

    // 팔로우 취소
    @Transactional
    public void deleteFollow(Long userId, Long followId) {
        validateUserId(userId);
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND));

        followRepository.delete(follow);
    }

    // 알림설정
    @Transactional
    public void toggleNotification(Long userId, Long followId) {
        validateUserId(userId);
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND));

        follow.toggleNotification();
    }

    //중복되는 검증로직 헬퍼메서드
    private void validateUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND);
        }
    }
}
