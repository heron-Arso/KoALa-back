package com.koala.koalaback;  // ← 이 패키지에 있어야 해요

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KoalaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KoalaApplication.class, args);
    }
}