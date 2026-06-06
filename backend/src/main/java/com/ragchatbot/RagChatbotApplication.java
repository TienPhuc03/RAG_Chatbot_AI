package com.ragchatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ragchatbot.config.LlmConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LlmConfig.class)
public class RagChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagChatbotApplication.class, args);
    }
}
