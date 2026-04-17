package com.fivefy.common.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class FullTextIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        executeIgnoreError("ALTER TABLE artists ADD FULLTEXT INDEX ft_artist_name (name) WITH PARSER ngram");
        executeIgnoreError("ALTER TABLE tracks ADD FULLTEXT INDEX ft_track_title (title) WITH PARSER ngram");
        executeIgnoreError("ALTER TABLE albums ADD FULLTEXT INDEX ft_album_title (title) WITH PARSER ngram");
    }

    private void executeIgnoreError(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("FULLTEXT 인덱스가 이미 존재합니다: {}", e.getMessage());
        }
    }
}
