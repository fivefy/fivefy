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
