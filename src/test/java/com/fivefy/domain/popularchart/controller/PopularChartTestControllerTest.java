package com.fivefy.domain.popularchart.controller;

import com.fivefy.common.config.security.JwtUtil;
import com.fivefy.common.docs.RestDocsSupport;
import com.fivefy.domain.popularchart.service.PopularChartGenerateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@WebMvcTest(PopularChartTestController.class)
@ActiveProfiles("local")
class PopularChartTestControllerTest extends RestDocsSupport {

    @MockitoBean private PopularChartGenerateService popularChartGenerateService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("테스트용 인기 차트 생성 API 호출 성공")
    void generate_success() throws Exception {
        // when & then
        mockMvc.perform(post("/test/popular-charts/generate")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"))
                .andDo(document("popular-chart-test-generate"));

        verify(popularChartGenerateService, times(1))
                .generateWeeklyChart(any(LocalDate.class));
    }
}
