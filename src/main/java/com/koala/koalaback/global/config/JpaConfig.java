package com.koala.koalaback.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정.
 * <p>
 * {@code @EnableJpaAuditing}을 메인 애플리케이션 클래스에 두면
 * entityManagerFactory가 너무 이른 시점에 초기화되어 Flyway와 순환 의존성
 * (Circular depends-on between 'flyway' and 'entityManagerFactory')이 발생한다.
 * 별도 @Configuration 클래스로 분리하여 이를 회피한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
