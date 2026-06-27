package com.ragchatbot.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QdrantProperties.class)
public class QdrantConfiguration {

    @Bean
    QdrantClient qdrantClient(QdrantProperties properties) {
        QdrantGrpcClient grpcClient = QdrantGrpcClient
                .newBuilder(
                        properties.getQdrantHost(),
                        properties.getQdrantPort(),
                        properties.isUseTls()
                )
                .withTimeout(properties.getRequestTimeout())
                .build();

        return new QdrantClient(grpcClient);
    }
}
