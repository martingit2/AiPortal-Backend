// src/main/java/com/AiPortal/config/DataSourceConfig.java
package com.AiPortal.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);

        // Vi setter pool-størrelsen eksplisitt til 1 for å jobbe med Supabase's Session Pooler.
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);

        // --- VIKTIGE ENDRINGER FOR LANGE BAKGRUNNSJOBBER ---

        // Øker Connection Timeout til 2 minutter.
        // Dette gir en jobb som venter på tilkoblingen mer tid før den gir opp.
        dataSource.setConnectionTimeout(120000); // 2 minutter (var 30 sekunder)

        // Øker Idle Timeout til 30 minutter.
        // Dette forhindrer at tilkoblingen blir lukket av poolen mens en lang
        // datainnsamlingsjobb holder på med eksterne API-kall (som kan ta tid).
        dataSource.setIdleTimeout(1800000); // 30 minutter (var 10 minutter)

        // Holder denne som den er. Nyttig for å oppdage feil.
        dataSource.setLeakDetectionThreshold(60000); // 1 minutt

        dataSource.setPoolName("Supabase-Hikari-Pool");

        return dataSource;
    }
}