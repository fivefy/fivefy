package com.fivefy.domain.popularchart.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.exception.BusinessException;
import com.fivefy.domain.popularchart.dto.response.PopularChartResponse;
import com.fivefy.domain.popularchart.enums.PopularChartErrorCode;
import com.fivefy.domain.popularchart.service.PopularChartGenerateService;
import com.fivefy.domain.popularchart.service.PopularChartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PopularChartController.class)
@AutoConfigureMockMvc(addFilters = false)
class PopularChartControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PopularChartService popularChartService;
    @MockitoBean private PopularChartGenerateService popularChartGenerateService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    @Nested
    @DisplayName("인기 차트 조회")
    class GetTop100 {

        @Test
        @DisplayName("snapshotDate 없이 조회 성공")
        void getTop100_withoutSnapshotDate_success() throws Exception {
            // given
            List<PopularChartResponse> response = List.of(
                    new PopularChartResponse(1L, 101L, 1, 300L, LocalDateTime.of(2026, 4, 13, 0, 0)),
                    new PopularChartResponse(2L, 102L, 2, 250L, LocalDateTime.of(2026, 4, 13, 0, 0))
            );

            given(popularChartService.getTop100(eq(null))).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/popular-charts/top100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("인기 차트 조회 성공"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1L))
                    .andExpect(jsonPath("$.data[0].trackId").value(101L))
                    .andExpect(jsonPath("$.data[0].rank").value(1))
                    .andExpect(jsonPath("$.data[0].playCount").value(300))
                    .andExpect(jsonPath("$.data[1].trackId").value(102L))
                    .andExpect(jsonPath("$.data[1].rank").value(2));
        }

        @Test
        @DisplayName("snapshotDate로 조회 성공")
        void getTop100_withSnapshotDate_success() throws Exception {
            // given
            LocalDate snapshotDate = LocalDate.of(2026, 4, 13);

            List<PopularChartResponse> response = List.of(
                    new PopularChartResponse(1L, 101L, 1, 300L, LocalDateTime.of(2026, 4, 13, 0, 0))
            );

            given(popularChartService.getTop100(eq(snapshotDate))).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/popular-charts/top100")
                            .param("snapshotDate", "2026-04-13"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("인기 차트 조회 성공"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(1L))
                    .andExpect(jsonPath("$.data[0].trackId").value(101L))
                    .andExpect(jsonPath("$.data[0].rank").value(1))
                    .andExpect(jsonPath("$.data[0].playCount").value(300));
        }

        @Test
        @DisplayName("차트 데이터가 없으면 404 반환")
        void getTop100_chartNotFound() throws Exception {
            // given
            given(popularChartService.getTop100(eq(null)))
                    .willThrow(new BusinessException(PopularChartErrorCode.CHART_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/popular-charts/top100"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message")
                            .value(PopularChartErrorCode.CHART_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("월요일이 아닌 snapshotDate로 조회하면 400 반환")
        void getTop100_invalidSnapshotDate() throws Exception {
            // given
            given(popularChartService.getTop100(eq(LocalDate.of(2026, 4, 7))))
                    .willThrow(new BusinessException(PopularChartErrorCode.INVALID_SNAPSHOT_DATE));

            // when & then
            mockMvc.perform(get("/api/popular-charts/top100")
                            .param("snapshotDate", "2026-04-07"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value(PopularChartErrorCode.INVALID_SNAPSHOT_DATE.getMessage()));
        }
    }
}
