package com.flightmonitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration for Swagger UI.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI flightMonitorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flight Price Monitor API")
                        .description("REST API for monitoring and tracking flight prices")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Flight Monitor Team")
                                .email("team@flightmonitor.com"))
                        .license(new License()
                                .name("MIT License")));
    }
}
