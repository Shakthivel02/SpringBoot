package com.app.store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableJpaAuditing // Enables @CreatedDate and @LastModifiedDate in BaseEntity
public class DatabaseSchedulerConfig {

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int connectionPoolSize;

    /**
     * Creates a custom bounded elastic scheduler for wrapping blocking JDBC calls.
     * The thread pool size is scaled according to your Hikari connection pool size
     * so that the scheduler threads do not outnumber the available database connections,
     * maintaining stable performance.
     */
    @Bean
    public Scheduler jdbcScheduler() {
        return Schedulers.newBoundedElastic(
                connectionPoolSize, 
                100000, 
                "jdbc-pool"
        );
    }
}
