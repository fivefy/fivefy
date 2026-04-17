package com.fivefy.domain.popularchart.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.domain.popularchart.service.PopularChartGenerateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PopularChartTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class PopularChartTestControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PopularChartGenerateService popularChartGenerateService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("테스트용 인기 차트 생성 API 호출 성공")
    void generate_success() throws Exception {
        // when & then
        mockMvc.perform(post("/test/popular-charts/generate"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(popularChartGenerateService, times(1))
                .generateWeeklyChart(any(LocalDate.class));
    }
}
