package com.fivefy.domain.artist.service;

import com.fivefy.domain.artist.dto.request.ArtistApplicationCreateRequest;
import com.fivefy.domain.artist.dto.response.ArtistApplicationCreateResponse;
import com.fivefy.domain.artist.dto.response.ArtistApplicationGetResponse;
import com.fivefy.domain.artist.entity.ArtistApplication;
import com.fivefy.domain.artist.repository.ArtistApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static com.fivefy.common.enums.ApplicationStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ArtistService의 비즈니스 로직을 검증하는 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ArtistApplicationRepository artistApplicationRepository;

    @InjectMocks
    private ArtistService artistService;

    @Nested
    @DisplayName("아티스트 등록 요청 생성")
    class CreateArtistApplication {

        @Test
        @DisplayName("아티스트 등록 요청 생성에 성공한다")
        void createArtistApplication_success() {
            // given
            Long userId = 1L;
            ArtistApplicationCreateRequest request = new ArtistApplicationCreateRequest(
                    "아이유",
                    "가수",
                    "https://example.com/profile.jpg"
            );

            ArtistApplication savedApplication = ArtistApplication.create(
                    userId,
                    request.requestedName(),
                    request.bio(),
                    request.profileImageUrl()
            );

            // 단위 테스트에서는 JPA auditing이 동작하지 않으므로 createdAt을 직접 주입한다.
            ReflectionTestUtils.setField(savedApplication, "id", 1L);
            ReflectionTestUtils.setField(savedApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 22, 30, 0));

            when(artistApplicationRepository.save(any(ArtistApplication.class)))
                    .thenReturn(savedApplication);

            // when
            ArtistApplicationCreateResponse response =
                    artistService.createArtistApplication(userId, request);

            // then
            assertThat(response.applicationId()).isEqualTo(1L);
            assertThat(response.requestedName()).isEqualTo("아이유");
            assertThat(response.status()).isEqualTo(PENDING.name());
            assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 4, 14, 22, 30, 0));

            verify(artistApplicationRepository, times(1))
                    .save(any(ArtistApplication.class));
        }
    }

    @Nested
    @DisplayName("내 아티스트 등록 요청 목록 조회")
    class GetMyArtistApplications {

        @Test
        @DisplayName("내 아티스트 등록 요청 목록을 최신순으로 조회한다")
        void getMyArtistApplications_success() {
            // given
            Long userId = 1L;

            ArtistApplication firstApplication = ArtistApplication.create(
                    userId,
                    "아이유",
                    "가수",
                    "https://example.com/iu.jpg"
            );
            ArtistApplication secondApplication = ArtistApplication.create(
                    userId,
                    "아이유 밴드",
                    "프로젝트 아티스트",
                    "https://example.com/band.jpg"
            );

            ReflectionTestUtils.setField(firstApplication, "id", 2L);
            ReflectionTestUtils.setField(firstApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 15, 10, 0, 0));

            ReflectionTestUtils.setField(secondApplication, "id", 1L);
            ReflectionTestUtils.setField(secondApplication, "createdAt",
                    LocalDateTime.of(2026, 4, 14, 10, 0, 0));

            when(artistApplicationRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(firstApplication, secondApplication));

            // when
            List<ArtistApplicationGetResponse> response = artistService.getMyArtistApplications(userId);

            // then
            assertThat(response).hasSize(2);
            assertThat(response.get(0).applicationId()).isEqualTo(2L);
            assertThat(response.get(0).requestedName()).isEqualTo("아이유");
            assertThat(response.get(0).status()).isEqualTo("PENDING");

            assertThat(response.get(1).applicationId()).isEqualTo(1L);
            assertThat(response.get(1).requestedName()).isEqualTo("아이유 밴드");

            verify(artistApplicationRepository, times(1))
                    .findAllByRequesterUserIdOrderByCreatedAtDesc(userId);
        }

        @Test
        @DisplayName("내 아티스트 등록 요청이 없으면 빈 목록을 반환한다")
        void getMyArtistApplications_empty() {
            // given
            Long userId = 1L;

            when(artistApplicationRepository.findAllByRequesterUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());

            // when
            List<ArtistApplicationGetResponse> response = artistService.getMyArtistApplications(userId);

            // then
            assertThat(response).isEmpty();

            verify(artistApplicationRepository, times(1))
                    .findAllByRequesterUserIdOrderByCreatedAtDesc(userId);
        }
    }
}