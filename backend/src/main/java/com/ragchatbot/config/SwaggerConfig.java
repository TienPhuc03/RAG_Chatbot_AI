package com.ragchatbot.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI ragChatbotOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("RAG Chatbot API")
                                .description(
                                        "Backend API for Vietnamese RAG Chatbot Research Project"
                                )
                                .version("v1.0")
                                .contact(
                                        new Contact()
                                                .name("RAG Chatbot Team")
                                )
                                .license(
                                        new License()
                                                .name("Apache 2.0")
                                )
                )
                .externalDocs(
                        new ExternalDocumentation()
                                .description("Project Documentation")
                );
    }
}