package com.AiPortal.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    // Henter verdier fra application.properties
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Setter de grunnleggende tilkoblingsdetaljene
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);


        // Vi setter pool-størrelsen manuelt og eksplisitt til 1.
        dataSource.setMaximumPoolSize(1);

        // Andre nyttige innstillinger for å unngå at forbindelsen blir "stale"
        dataSource.setMinimumIdle(1);
        dataSource.setIdleTimeout(600000); // 10 minutter
        dataSource.setConnectionTimeout(30000); // 30 sekunder
        dataSource.setLeakDetectionThreshold(60000); // 1 minutt for å oppdage "lekkede" forbindelser

        // Navn for poolen, nyttig for logging
        dataSource.setPoolName("Supabase-Hikari-Pool");

        return dataSource;
    }
}