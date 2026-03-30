package com.koala.koalaback;  // ← 이 패키지에 있어야 해요

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class KoalaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KoalaApplication.class, args);
    }
}