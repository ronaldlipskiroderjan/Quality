package br.com.grupo5.Quality.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI qualityOpenApi() {
        SecurityScheme esquemaJwt = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("Quality API")
                        .description("API REST para gestão de planos e auditorias de qualidade.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, esquemaJwt))
                .addSecurityItem(new SecurityRequirement()
                        .addList(ESQUEMA_JWT));
    }
}
