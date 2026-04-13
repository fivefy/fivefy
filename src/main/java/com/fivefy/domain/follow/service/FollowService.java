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

    @Transactional
    public FollowCreateResponse createFollow(Long userId, Long artistId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND);
        }
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

    @Transactional(readOnly = true)
    public List<FollowGetResponse> getFollows(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND);
        }

        return followRepository.findAllByUserId(userId)
                .stream()
                .map(FollowGetResponse::from)
                .toList();
    }

    @Transactional
    public void deleteFollow(Long userId, Long followId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND);
        }

        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new BusinessException(FollowErrorCode.ERR_FOLLOW_NOT_FOUND));

        followRepository.delete(follow);
    }
}
