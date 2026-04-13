package com.fivefy.domain.follow.service;

import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.entity.Follow;
import com.fivefy.domain.follow.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;

    @Transactional
    public FollowCreateResponse createFollow(Long userId, Long artistId) {
        if (followRepository.existsByUserIdAndArtistId(userId, artistId)) {
            throw new IllegalStateException("이미 팔로우한 아티스트입니다");
        }

        Follow follow = Follow.create(artistId, userId);
        followRepository.save(follow);

        return FollowCreateResponse.from(follow);
    }
}
