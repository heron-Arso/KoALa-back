package com.koala.koalaback.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    /**
     * 로컬 개발 환경: 업로드된 파일을 /uploads/** 경로로 서빙
     */
    @Bean
    @Profile("local")
    public WebMvcConfigurer localUploadResourceHandler(
            @Value("${koala.storage.upload-dir:./uploads}") String uploadDir) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // 절대경로로 변환 (./uploads → /absolute/path/uploads/)
                String absolutePath = new File(uploadDir).getAbsolutePath() + "/";
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:" + absolutePath);
            }
        };
    }
}

