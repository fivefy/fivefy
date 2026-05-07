package com.fivefy.common.config.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(name = "mysqlFlywayBean")
    @ConditionalOnProperty(name = "custom.flyway.mysql.enabled", havingValue = "true")
    public Flyway mysqlFlyway(@Qualifier("primaryDataSource") DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/mysql")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean(name = "postgresFlywayBean")
    @ConditionalOnProperty(name = "custom.flyway.postgre.enabled", havingValue = "true")
    public Flyway postgresFlyway(@Qualifier("vectorDataSource") DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/postgresql")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(true)
                .outOfOrder(false)
                .load();
        flyway.migrate();
        return flyway;
    }
}
