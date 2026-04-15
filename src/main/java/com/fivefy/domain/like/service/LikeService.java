package com.fivefy.domain.like.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.like.dto.response.LikeCreateResponse;
import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.entity.Like;
import com.fivefy.domain.like.enums.LikeErrorCode;
import com.fivefy.domain.like.enums.TargetType;
import com.fivefy.domain.like.repository.LikeRepository;
import com.fivefy.domain.track.enums.TrackErrorCode;
import com.fivefy.domain.track.repository.TrackRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// TODO: Track/Album 삭제 시 연관 Like 삭제 처리 필요 / 방안: 이벤트 방식

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;

    @Transactional
    public LikeCreateResponse createLike(Long targetId, TargetType targetType, Long userId) {
        User user = getUser(userId);
        Long validatedTargetId = getValidatedTargetId(targetId, targetType);

        // 중복 방지
        if (likeRepository.existsByUserIdAndTargetIdAndTargetType(userId, validatedTargetId, targetType)) {
            throw new BusinessException(LikeErrorCode.ERR_LIKE_ALREADY_EXISTS);
        }

        try {
            Like like = Like.create(user.getId(), validatedTargetId, targetType);
            likeRepository.save(like);
            return LikeCreateResponse.from(like);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(LikeErrorCode.ERR_LIKE_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public Page<LikeGetResponse> getLikes(Long userId, TargetType targetType, Pageable pageable) {
        getUser(userId);

        return likeRepository.findLikesWithTarget(userId, targetType, pageable);
    }

    @Transactional
    public void deleteLike(Long userId, Long likeId) {
        getUser(userId);

        Like like = likeRepository.findByIdAndUserId(likeId, userId)
                .orElseThrow(() -> new BusinessException(LikeErrorCode.ERR_LIKE_NOT_FOUND));

        likeRepository.delete(like);
    }

    // 검증 헬퍼 메서드
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    private Long getValidatedTargetId(Long targetId, TargetType targetType) {
        return switch (targetType) {
            case TRACK -> trackRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND))
                    .getId();
            case ALBUM -> albumRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND))
                    .getId();
        };
    }
}
