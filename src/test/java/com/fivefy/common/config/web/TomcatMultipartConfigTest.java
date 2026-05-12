package com.fivefy.common.config.web;

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class TomcatMultipartConfigTest {

    @Test
    @DisplayName("multipart part header 크기 제한을 확장한다")
    void tomcatMultipartCustomizer_success() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        Connector connector = new Connector();

        new TomcatMultipartConfig()
                .tomcatMultipartCustomizer()
                .customize(factory);

        factory.getConnectorCustomizers()
                .forEach(customizer -> customizer.customize(connector));

        assertThat(connector.getMaxPartHeaderSize()).isEqualTo(16 * 1024);
    }
}
