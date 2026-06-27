package com.ragchatbot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rag.vector-store")
public class QdrantProperties {

    @NotBlank
    private String qdrantHost = "localhost";

    @Min(1)
    private int qdrantPort = 6333;

    @NotBlank
    private String collectionName = "rag_chunks";

    @Min(1)
    private int vectorSize = 3072;

    private boolean useTls = false;

    private Duration requestTimeout = Duration.ofSeconds(30);

    public String getQdrantHost() {
        return qdrantHost;
    }

    public void setQdrantHost(String qdrantHost) {
        this.qdrantHost = qdrantHost;
    }

    public int getQdrantPort() {
        return qdrantPort;
    }

    public void setQdrantPort(int qdrantPort) {
        this.qdrantPort = qdrantPort;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public int getVectorSize() {
        return vectorSize;
    }

    public void setVectorSize(int vectorSize) {
        this.vectorSize = vectorSize;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
