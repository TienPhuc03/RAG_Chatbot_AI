package com.ragchatbot;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public abstract class PostgresIntegrationTestSupport {

    // private static final DockerImageName PGVECTOR_IMAGE =
    //         DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres");

    // @Container
    // protected static final PostgreSQLContainer<?> POSTGRES =
    //         new PostgreSQLContainer<>(PGVECTOR_IMAGE)
    //                 .withDatabaseName("ragchatbot")
    //                 .withUsername("raguser")
    //                 .withPassword("ragpass");

    // static {
    //     POSTGRES.start();
    // }

    @DynamicPropertySource
    // static void registerProperties(DynamicPropertyRegistry registry) {
    //     registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    //     registry.add("spring.datasource.username", POSTGRES::getUsername);
    //     registry.add("spring.datasource.password", POSTGRES::getPassword);
    //     registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    //     registry.add("spring.flyway.enabled", () -> "true");
    // }
    static void registerProperties(DynamicPropertyRegistry registry) {
        // Ép Spring Boot kết nối trực tiếp vào container Postgres thật đang chạy trên Docker Desktop của bạn
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/ragchatbot");
        registry.add("spring.datasource.username", () -> "raguser");
        registry.add("spring.datasource.password", () -> "ragpass");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.flyway.enabled", () -> "true");
        // THÊM DÒNG NÀY: Ép Flyway tự động tạo bảng schema_history mà không vứt exception sập context
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
    }
}
