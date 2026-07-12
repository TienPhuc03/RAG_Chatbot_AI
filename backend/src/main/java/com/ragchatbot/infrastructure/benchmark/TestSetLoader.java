package com.ragchatbot.infrastructure.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.domain.model.RelevantSource;
import com.ragchatbot.domain.model.TestCase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class TestSetLoader {

    private static final String TEST_SET_PATH =
            "static/test-data/test_set.json";

    private final ObjectMapper objectMapper;

    public TestSetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TestCase> loadTestCases() {
        try {
            ClassPathResource resource =
                    new ClassPathResource(TEST_SET_PATH);

            try (InputStream inputStream = resource.getInputStream()) {
                List<TestCase> testCases = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<TestCase>>() {
                        }
                );

                validateTestCases(testCases);
                return testCases;
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Lỗi khi đọc và xử lý file test_set.json: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private void validateTestCases(List<TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            throw new IllegalStateException(
                    "Test set trống hoặc không tìm thấy dữ liệu."
            );
        }

        Set<String> seenIds = new HashSet<>();

        for (TestCase testCase : testCases) {
            validateTestCase(testCase, seenIds);
        }
    }

    private void validateTestCase(
            TestCase testCase,
            Set<String> seenIds
    ) {
        if (testCase == null) {
            throw new IllegalStateException(
                    "Test set chứa một phần tử null."
            );
        }

        requireText(testCase.id(), "id", testCase.id());
        requireText(
                testCase.question(),
                "question",
                testCase.id()
        );
        requireText(
                testCase.groundTruth(),
                "groundTruth",
                testCase.id()
        );
        requireText(
                testCase.category(),
                "category",
                testCase.id()
        );

        if (!seenIds.add(testCase.id().trim())) {
            throw new IllegalStateException(
                    "Trùng id trong test set: " + testCase.id()
            );
        }

        boolean outOfScope =
                Boolean.TRUE.equals(testCase.outOfScope());

        if (outOfScope) {
            return;
        }

        if (testCase.relevantSources() == null
                || testCase.relevantSources().isEmpty()) {
            throw new IllegalStateException(
                    "TestCase thiếu relevantSources tại id: "
                            + testCase.id()
            );
        }

        for (RelevantSource source
                : testCase.relevantSources()) {
            validateRelevantSource(source, testCase.id());
        }
    }

    private void validateRelevantSource(
            RelevantSource source,
            String testCaseId
    ) {
        if (source == null) {
            throw new IllegalStateException(
                    "RelevantSource bị null tại id: "
                            + testCaseId
            );
        }

        boolean hasSourceFileName =
                StringUtils.hasText(source.sourceFileName());

        boolean hasRealDocumentId =
                isUuid(source.documentId());

        /*
         * Ưu tiên sourceFileName vì documentId thay đổi sau mỗi lần
         * re-index. Các giá trị như document_01 chỉ là placeholder.
         */
        if (!hasSourceFileName && !hasRealDocumentId) {
            throw new IllegalStateException(
                    "RelevantSource phải có sourceFileName "
                            + "hoặc documentId UUID hợp lệ tại id: "
                            + testCaseId
            );
        }

        boolean hasPage =
                source.pageStart() != null
                        || source.pageEnd() != null;

        boolean hasSection =
                StringUtils.hasText(source.section());

        if (!hasPage && !hasSection) {
            throw new IllegalStateException(
                    "RelevantSource cần ít nhất pageStart/pageEnd "
                            + "hoặc section tại id: "
                            + testCaseId
            );
        }

        validatePageRange(source, testCaseId);
    }

    private void validatePageRange(
            RelevantSource source,
            String testCaseId
    ) {
        Integer pageStart = source.pageStart();
        Integer pageEnd = source.pageEnd();

        if (pageStart != null && pageStart < 1) {
            throw new IllegalStateException(
                    "pageStart phải lớn hơn hoặc bằng 1 tại id: "
                            + testCaseId
            );
        }

        if (pageEnd != null && pageEnd < 1) {
            throw new IllegalStateException(
                    "pageEnd phải lớn hơn hoặc bằng 1 tại id: "
                            + testCaseId
            );
        }

        if (pageStart != null
                && pageEnd != null
                && pageStart > pageEnd) {
            throw new IllegalStateException(
                    "pageStart không được lớn hơn pageEnd tại id: "
                            + testCaseId
            );
        }
    }

    private void requireText(
            String value,
            String fieldName,
            String testCaseId
    ) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "TestCase thiếu " + fieldName
                            + " tại id: " + testCaseId
            );
        }
    }

    private boolean isUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}