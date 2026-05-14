package com.fivefy.common.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * 로컬 오디오 파일 정적 리소스 설정
 */
@Configuration
@EnableConfigurationProperties(AudioStorageProperties.class)
@ConditionalOnProperty(name = "storage.audio.type", havingValue = "local")
public class LocalAudioResourceConfig implements WebMvcConfigurer {

    private final AudioStorageProperties properties;

    public LocalAudioResourceConfig(AudioStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path storageRoot = Path.of(properties.normalizedLocalRoot())
                .resolve(properties.normalizedPrefix())
                .toAbsolutePath()
                .normalize();
        String location = storageRoot.toUri().toString() + "/";

        registry.addResourceHandler("/" + properties.normalizedPrefix() + "/**")
                .addResourceLocations(location);
    }
}