package com.koala.koalaback;  // ← 이 패키지에 있어야 해요

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableJpaAuditing 은 JpaConfig 로 분리 (Flyway 순환 의존성 회피)
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class KoalaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KoalaApplication.class, args);
    }
}