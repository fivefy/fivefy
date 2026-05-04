package com.fivefy.common.config.data;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * AI/벡터 전용 PostgreSQL DataSource.
 *
 * JPA를 안 쓰고 JdbcTemplate만 쓰는 이유:
 * - pgvector의 VECTOR 타입은 Hibernate가 기본 지원 안 함 (커스텀 타입 컨버터 필요)
 * - 임베딩 CRUD는 단순한 INSERT/SELECT가 전부라 JPA 오버헤드가 불필요
 * - JdbcTemplate + PGobject로 직관적으로 처리
 */
@Configuration
public class VectorDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.vector")
    public DataSource vectorDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate vectorNamedJdbcTemplate(@Qualifier("vectorDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager vectorTransactionManager(
            @Qualifier("vectorDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
