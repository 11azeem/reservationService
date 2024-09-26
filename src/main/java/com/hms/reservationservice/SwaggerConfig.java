package com.hms.reservationservice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI apiInfo() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Reservation Service Rest Api")
                                .description("Rest Api for Reservation Service of Suite Spott")
                                .version("1.0"));
    }
}
