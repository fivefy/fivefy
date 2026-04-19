package com.fivefy.domain.track.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.track.dto.request.FreeTrackApplicationCreateRequest;
import com.fivefy.domain.track.dto.response.TrackApplicationResponse;
import com.fivefy.domain.track.entity.TrackApplication;
import com.fivefy.domain.track.enums.TrackApplicationErrorCode;
import com.fivefy.domain.track.enums.TrackType;
import com.fivefy.domain.track.repository.TrackApplicationRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랙 도메인 서비스
 */
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackApplicationRepository trackApplicationRepository;
    private final UserRepository userRepository;

    /**
     * 자유 창작 트랙 등록 신청
     */
    @Transactional
    public TrackApplicationResponse createFreeTrackApplication(
            Long userId,
            FreeTrackApplicationCreateRequest request
    ) {
        // 신청 유저 존재 확인
        findUser(userId);

        // 자유 창작 PENDING 중복 신청 검증
        validateDuplicateFreeCreationApplication(userId, request.title(), request.audioUrl());

        // 등록 신청 생성 및 저장
        TrackApplication savedApplication = trackApplicationRepository.save(
                TrackApplication.create(
                        userId,
                        TrackType.FREE_CREATION,
                        null,
                        null,
                        null,
                        request.title(),
                        request.lyrics(),
                        request.genre(),
                        request.audioUrl(),
                        request.durationSec(),
                        null,
                        null
                )
        );

        return TrackApplicationResponse.from(savedApplication);
    }

    // 유저 조회
    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ERR_USER_NOT_FOUND));
    }

    // 자유 창작 중복 신청 검증
    private void validateDuplicateFreeCreationApplication(
            Long requesterUserId,
            String title,
            String audioUrl
    ) {
        if (trackApplicationRepository.existsPendingFreeCreationApplication(
                requesterUserId,
                title,
                audioUrl
        )) {
            throw new BusinessException(
                    TrackApplicationErrorCode.ERR_TRACK_APPLICATION_ALREADY_EXISTS
            );
        }
    }
}