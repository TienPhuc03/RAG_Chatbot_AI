package com.ragchatbot.infrastructure.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.domain.model.ManualTestCase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class ManualTestSetLoader {

    private final ObjectMapper objectMapper;

    public ManualTestSetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ManualTestCase> loadTestCases() {
        try {
            ClassPathResource resource = new ClassPathResource("static/test-data/test_set_manual.json");
            try (InputStream inputStream = resource.getInputStream()) {
                return objectMapper.readValue(inputStream, new TypeReference<List<ManualTestCase>>() {});
            }
        } catch (Exception e) {
            throw new IllegalStateException("Loi khi doc test_set_manual.json: " + e.getMessage(), e);
        }
    }
}