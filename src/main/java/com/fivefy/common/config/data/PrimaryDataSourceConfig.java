package com.fivefy.common.config.data;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 기존 fivefy 도메인용 MySQL DataSource (Primary).
 * 새로 추가하는 클래스이지만 기존 도메인의 JPA Repository 들이
 * 이 EntityManager를 자동으로 쓰도록 @Primary 로 지정.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.fivefy.domain", // 기존 fivefy의 JPA Repository 패키지들
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().type(com.zaxxer.hikari.HikariDataSource.class).build();
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("primaryDataSource") DataSource dataSource) {

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.put("hibernate.hbm2ddl.auto", "update");

        return builder
                .dataSource(dataSource)
                .packages("com.fivefy.domain") // 기존 엔티티들
                .persistenceUnit("primary")
                .properties(props)
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    /**
     * 기존 도메인 + AI 도메인의 raw SQL 조작용 JdbcTemplate (MySQL).
     *
     * @Primary 적용 이유:
     *   AI 도메인의 6개 서비스가 모두 MySQL JdbcTemplate을 쓰는데,
     *   매번 @Qualifier 다는 건 보일러플레이트.
     *   - primaryJdbcTemplate: @Primary → 그냥 JdbcTemplate 타입 주입 시 자동 선택
     *   - vectorJdbcTemplate: 빈 이름이 필드명과 일치 → Spring의 byName 매칭으로 주입
     *
     * 이렇게 두면 서비스 코드는 깔끔하게 `private final JdbcTemplate primaryJdbcTemplate;`
     * 또는 `private final JdbcTemplate vectorJdbcTemplate;` 만 쓰면 됨.
     */
    @Primary
    @Bean
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
