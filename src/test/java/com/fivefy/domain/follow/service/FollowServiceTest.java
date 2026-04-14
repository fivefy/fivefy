package com.fivefy.domain.follow.service;

import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.artist.entity.Artist;
import com.fivefy.domain.artist.enums.ArtistExceptionEnum;
import com.fivefy.domain.artist.repository.ArtistRepository;
import com.fivefy.domain.follow.dto.response.FollowCreateResponse;
import com.fivefy.domain.follow.dto.response.FollowGetResponse;
import com.fivefy.domain.follow.entity.Follow;
import com.fivefy.domain.follow.enums.FollowErrorCode;
import com.fivefy.domain.follow.repository.FollowRepository;
import com.fivefy.domain.user.entity.User;
import com.fivefy.domain.user.enums.UserErrorCode;
import com.fivefy.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private FollowService followService;

    private User mockUser;
    private Artist mockArtist;
    private Follow mockFollow;

    private static final Long USER_ID = 1L;
    private static final Long ARTIST_ID = 2L;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        mockArtist = mock(Artist.class);
        mockFollow = Follow.create(ARTIST_ID, USER_ID);

        lenient().when(mockUser.getId()).thenReturn(USER_ID);
        lenient().when(mockArtist.getId()).thenReturn(ARTIST_ID);
    }

    // createFollow 성공
    @Test
    @DisplayName("팔로우 등록 성공")
    void createFollow_success() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(mockArtist));
        given(followRepository.existsByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(false);
        given(followRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        FollowCreateResponse response = followService.createFollow(USER_ID, ARTIST_ID);

        // then
        assertThat(response.artistId()).isEqualTo(ARTIST_ID);
        assertThat(response.notificationEnabled()).isTrue();
        verify(followRepository).save(any(Follow.class));
    }

    // createFollow 실패
    @Test
    @DisplayName("팔로우 등록 실패 - 존재하지 않는 유저")
    void createFollow_userNotFound() {
        // given
        given(userRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.createFollow(USER_ID, ARTIST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("팔로우 등록 실패 - 존재하지 않는 아티스트")
    void createFollow_artistNotFound() {
        // given
        given(userRepository.findById(any())).willReturn(Optional.of(mockUser));
        given(artistRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.createFollow(USER_ID, ARTIST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ArtistExceptionEnum.ERR_ARTIST_NOT_FOUND.getMessage());
    }

    // createFollow 중복 체크
    @Test
    @DisplayName("팔로우 등록 실패 - 중복 팔로우 (existsBy 검증)")
    void createFollow_duplicateByExists() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(mockArtist));
        given(followRepository.existsByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> followService.createFollow(USER_ID, ARTIST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS.getMessage());

        verify(followRepository, never()).save(any());
    }

    @Test
    @DisplayName("팔로우 등록 실패 - 중복 팔로우 (DB UniqueConstraint 위반)")
    void createFollow_duplicateByDBConstraint() {
        // given
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(artistRepository.findById(ARTIST_ID)).willReturn(Optional.of(mockArtist));
        given(followRepository.existsByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(false);
        given(followRepository.save(any())).willThrow(DataIntegrityViolationException.class);

        // when & then
        assertThatThrownBy(() -> followService.createFollow(USER_ID, ARTIST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FollowErrorCode.ERR_FOLLOW_ALREADY_EXISTS.getMessage());
    }

    // getFollows 페이징 조회
    @Test
    @DisplayName("팔로우 목록 조회 성공 - 페이징")
    void getFollows_success() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Follow> followPage = new PageImpl<>(List.of(mockFollow), pageable, 1);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(followRepository.findAllByUserId(USER_ID, pageable)).willReturn(followPage);

        // when
        Page<FollowGetResponse> responses = followService.getFollows(USER_ID, pageable);

        // then
        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getTotalElements()).isEqualTo(1);
        assertThat(responses.getContent().get(0).artistId()).isEqualTo(ARTIST_ID);
    }

    @Test
    @DisplayName("팔로우 목록 조회 실패 - 존재하지 않는 유저")
    void getFollows_userNotFound() {
        // given
        given(userRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.getFollows(USER_ID, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.ERR_USER_NOT_FOUND.getMessage());
    }

    // deleteFollow
    @Test
    @DisplayName("팔로우 취소 성공")
    void deleteFollow_success() {
        // given
        given(followRepository.findByUserIdAndArtistId(USER_ID, ARTIST_ID)).willReturn(Optional.of(mockFollow));

        // when
        followService.deleteFollow(USER_ID, ARTIST_ID);

        // then
        verify(followRepository).delete(mockFollow);
    }

    @Test
    @DisplayName("팔로우 취소 실패 - 팔로우 없음")
    void deleteFollow_notFound() {
        // given
        given(followRepository.findByUserIdAndArtistId(any(), any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.deleteFollow(USER_ID, ARTIST_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FollowErrorCode.ERR_FOLLOW_NOT_FOUND.getMessage());
    }
}
