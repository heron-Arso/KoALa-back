package com.koala.koalaback.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // Java 8+ 날짜/시간을 타임스탬프가 아닌 ISO-8601 문자열로 직렬화
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 알 수 없는 JSON 필드가 있어도 역직렬화 실패 방지
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
