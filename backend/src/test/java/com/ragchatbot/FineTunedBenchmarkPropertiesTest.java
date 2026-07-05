package com.ragchatbot;

import static org.assertj.core.api.Assertions.assertThat;

import com.ragchatbot.config.FineTunedBenchmarkProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class FineTunedBenchmarkPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "rag.benchmark.finetune.ollama-base-url=http://ollama-finetuned:11434",
                    "rag.benchmark.finetune.ollama-model=hoc-phan-chatbot-ft",
                    "rag.benchmark.finetune.timeout=95s"
            );

    @Test
    void bindsFineTunedBenchmarkProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FineTunedBenchmarkProperties.class);

            FineTunedBenchmarkProperties properties = context.getBean(FineTunedBenchmarkProperties.class);
            assertThat(properties.getOllamaBaseUrl()).isEqualTo("http://ollama-finetuned:11434");
            assertThat(properties.getOllamaModel()).isEqualTo("hoc-phan-chatbot-ft");
            assertThat(properties.getTimeout()).hasSeconds(95);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FineTunedBenchmarkProperties.class)
    static class TestConfig {
    }
}
