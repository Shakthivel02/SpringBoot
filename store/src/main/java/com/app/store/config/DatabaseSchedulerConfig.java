package com.app.store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableJpaAuditing 
public class DatabaseSchedulerConfig {

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int connectionPoolSize;

    @Bean
    public Scheduler jdbcScheduler() {
        return Schedulers.newBoundedElastic(
                connectionPoolSize, 
                100000, 
                "jdbc-pool"
        );
    }
}
