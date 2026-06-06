package com.ragchatbot.infrastructure.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.domain.model.TestCase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class TestSetLoader {

    private final ObjectMapper objectMapper;

    // Spring Boot sẽ tự động inject ObjectMapper vào đây
    public TestSetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TestCase> loadTestCases() {
        try {
            // Chỉ định đường dẫn tới file JSON trong thư mục resources
            ClassPathResource resource = new ClassPathResource("static/test-data/test_set.json");

            try (InputStream inputStream = resource.getInputStream()) {
                // Đọc file JSON và ép kiểu thành danh sách các đối tượng TestCase
                List<TestCase> testCases = objectMapper.readValue(inputStream, new TypeReference<List<TestCase>>() {});

                // Validate 1: Kiểm tra danh sách không được null hoặc rỗng
                if (testCases == null || testCases.isEmpty()) {
                    throw new IllegalStateException("Test set trống hoặc không tìm thấy dữ liệu trong file.");
                }

                // Validate 2: Kiểm tra từng TestCase không được thiếu trường dữ liệu nào
                for (TestCase testCase : testCases) {
                    if (testCase.id() == null || testCase.question() == null ||
                            testCase.groundTruth() == null || testCase.category() == null) {
                        throw new IllegalStateException("Sai định dạng: TestCase chứa giá trị null tại id: " + testCase.id());
                    }
                }

                return testCases;
            }
        } catch (Exception e) {
            // Bắt mọi lỗi (ví dụ: không tìm thấy file, sai format JSON) và ném ra IllegalStateException
            throw new IllegalStateException("Lỗi khi đọc và xử lý file test_set.json: " + e.getMessage(), e);
        }
    }
}