package com.kh.finalproject.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SpringDocConfiguration {

    @Bean
    public OpenAPI openAPI() {
        String jwtHeaderName = "Authorization";

        Info info = new Info()
                .title("Contents REST API")
                .description("ReactJS와 통신하기 위한 모든 REST service 정보")
                .version("0.0.1");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtHeaderName);

        Components components = new Components()
                .addSecuritySchemes(jwtHeaderName,
                        new SecurityScheme()
                                .name(jwtHeaderName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                );

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
