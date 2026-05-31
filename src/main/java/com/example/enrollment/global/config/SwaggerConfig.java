package com.example.enrollment.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("수강 신청 시스템 API")
                        .description("프로덕트 엔지니어 채용 과제 - 수강 신청 시스템")
                        .version("v1.0.0"));
    }
}