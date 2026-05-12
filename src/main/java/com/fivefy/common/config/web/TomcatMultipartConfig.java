package com.fivefy.common.config.web;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatMultipartConfig {

    private static final int MAX_PART_HEADER_SIZE = 16 * 1024;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMultipartCustomizer() {
        return factory -> factory.addConnectorCustomizers(
                connector -> connector.setMaxPartHeaderSize(MAX_PART_HEADER_SIZE)
        );
    }
}
