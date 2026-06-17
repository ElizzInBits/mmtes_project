package com.horacerta.backend.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HoraCerta API")
                        .version("1.0")
                        .description("API do Orquestrador de Agendamentos HoraCerta")
                        .contact(new Contact()
                                .name("Equipe HoraCerta")
                                .email("suporte@horacerta.com")));
    }
}
