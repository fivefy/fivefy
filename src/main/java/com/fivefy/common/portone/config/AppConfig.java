package com.fivefy.common.portone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // RestTemplate Bean 등록(Application에 EnableConfigurationProperties 추가하기)
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


