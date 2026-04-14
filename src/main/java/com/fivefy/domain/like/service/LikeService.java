package com.fivefy.domain.like.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.album.entity.Album;
import com.fivefy.domain.artist.enums.ArtistErrorCode;
import com.fivefy.domain.album.enums.AlbumErrorCode;
import com.fivefy.domain.album.repository.AlbumRepository;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.like.dto.response.LikeCreateResponse;
import com.fivefy.domain.like.dto.response.LikeGetResponse;
import com.fivefy.domain.like.entity.Like;
import com.fivefy.domain.like.enums.LikeErrorCode;
import com.fivefy.domain.like.enums.TargetType;
import com.fivefy.domain.like.repository.LikeRepository;
import com.fivefy.domain.track.entity.Track;
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

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;

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
    public Page<LikeGetResponse> getLikes(Long userId, Pageable pageable) {
        getUser(userId);

        return likeRepository.findAllByUserId(userId, pageable)
                .map(like -> {
                    if (like.getTargetType() == TargetType.TRACK) {
                        Track track = trackRepository.findById(like.getTargetId())
                                .orElseThrow(() -> new BusinessException(TrackErrorCode.ERR_TRACK_NOT_FOUND));
                        String artistName = getArtistName(track.getArtistId());
                        return LikeGetResponse.from(like, track.getTitle(), artistName);
                    } else {
                        Album album = albumRepository.findById(like.getTargetId())
                                .orElseThrow(() -> new BusinessException(AlbumErrorCode.ERR_ALBUM_NOT_FOUND));
                        String artistName = getArtistName(album.getArtistId());
                        return LikeGetResponse.from(like, album.getTitle(), artistName);
                    }
                });
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

    private String getArtistName(Long artistId) {
        if (artistId == null) return null;
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new BusinessException(ArtistErrorCode.ERR_ARTIST_NOT_FOUND))
                .getName();
    }
}
