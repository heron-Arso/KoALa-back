package com.koala.koalaback.global.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 수동 설정.
 *
 * <p>Spring Boot 4.x 의 {@code FlywayAutoConfiguration} 은 JPA(entityManagerFactory)와
 * 양방향 depends-on 관계를 만들어 부팅 시
 * "Circular depends-on relationship between 'flyway' and 'entityManagerFactory'" 로 실패한다.
 * (jasypt 제거, @EnableJpaAuditing 분리, ddl-auto=none 모두 시도했으나 자동설정 자체가 원인이라 해결되지 않음)
 *
 * <p>따라서 자동설정(flyway-core 단독은 SB4 에서 autoconfigure 되지 않음)에 의존하지 않고
 * Flyway 빈을 직접 등록하여 부팅 시 {@code migrate()} 를 실행한다.
 * 이 빈은 {@link DataSource} 에만 의존하므로 entityManagerFactory 와 순환이 발생하지 않는다.
 *
 * <p>설정값은 기존 {@code spring.flyway.*} 프로퍼티(application.yml / 명령행 인자)를 그대로 읽는다.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(
            DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String[] locations,
            @Value("${spring.flyway.baseline-on-migrate:true}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:14}") String baselineVersion,
            @Value("${spring.flyway.validate-on-migrate:false}") boolean validateOnMigrate
    ) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .validateOnMigrate(validateOnMigrate)
                .load();
    }
}
