package com.fivefy.common.initializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FullTextIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE artists ADD FULLTEXT INDEX ft_artist_name (name) WITH PARSER ngram");
        jdbcTemplate.execute("ALTER TABLE tracks ADD FULLTEXT INDEX ft_track_title (title) WITH PARSER ngram");
        jdbcTemplate.execute("ALTER TABLE albums ADD FULLTEXT INDEX ft_album_title (title) WITH PARSER ngram");
    }
}