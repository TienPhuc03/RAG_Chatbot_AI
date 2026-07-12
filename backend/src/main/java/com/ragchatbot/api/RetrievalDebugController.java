package com.ragchatbot.api;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/benchmark/debug")
@Tag(
        name = "Benchmark Debug",
        description = "API kiểm tra retrieval top-k mà không gọi LLM hoặc RAGAS"
)
public class RetrievalDebugController {

    private final EmbeddingRouter embeddingRouter;
    private final VectorStoreService vectorStoreService;

    public RetrievalDebugController(
            EmbeddingRouter embeddingRouter,
            VectorStoreService vectorStoreService
    ) {
        this.embeddingRouter = embeddingRouter;
        this.vectorStoreService = vectorStoreService;
    }

    @Operation(
            summary = "Kiểm tra retrieval top-k",
            description = """
                    Tạo embedding cho câu hỏi và tìm top-k chunk trong Qdrant.
                    API này không sinh câu trả lời, không gọi Gemini và không gọi RAGAS.
                    """
    )
    @GetMapping("/retrieval")
    public RetrievalDebugResponse retrieve(
            @RequestParam
            String question,

            @RequestParam(defaultValue = "BGE_M3")
            EmbeddingModel embeddingModel,

            @RequestParam(defaultValue = "SEMANTIC")
            ChunkingStrategy chunkingStrategy,

            @RequestParam(defaultValue = "5")
            int topK,

            @RequestParam(required = false)
            String courseCode,

            @RequestParam(required = false)
            String chapterCode
    ) {
        validateRequest(question, embeddingModel, topK);

        String normalizedQuestion = question.trim();
        String normalizedCourseCode = normalize(courseCode);
        String normalizedChapterCode = normalize(chapterCode);

        /*
         * Bước 1: tạo embedding cho câu hỏi.
         */
        long embeddingStartedAt = System.nanoTime();

        List<Float> queryEmbedding = embeddingRouter.embed(
                embeddingModel,
                normalizedQuestion
        );

        long embeddingLatencyMs = elapsedMillis(embeddingStartedAt);

        /*
         * Bước 2: tìm top-k chunk trong Qdrant.
         */
        long searchStartedAt = System.nanoTime();

        List<RetrievedContext> contexts = vectorStoreService.search(
                embeddingModel,
                queryEmbedding,
                topK,
                chunkingStrategy,
                normalizedCourseCode,
                normalizedChapterCode,
                null
        );

        long searchLatencyMs = elapsedMillis(searchStartedAt);

        /*
         * Bước 3: thêm rank vào từng kết quả.
         */
        List<RetrievalDebugItem> results =
        new java.util.ArrayList<>(contexts.size());
        for (int index = 0; index < contexts.size(); index++) {
            results.add(
                    toItem(
                            index + 1,
                            contexts.get(index)
                    )
            );
        }

        return new RetrievalDebugResponse(
                normalizedQuestion,
                embeddingModel,
                chunkingStrategy,
                topK,
                normalizedCourseCode,
                normalizedChapterCode,
                embeddingLatencyMs,
                searchLatencyMs,
                results.size(),
                results
        );
    }

    private RetrievalDebugItem toItem(
            int rank,
            RetrievedContext context
    ) {
        return new RetrievalDebugItem(
                rank,
                context.chunkId(),
                context.documentId(),
                context.sourceFileName(),
                context.courseCode(),
                context.chapterCode(),
                context.pageNumber(),
                context.pageStart(),
                context.pageEnd(),
                context.section(),
                context.score(),
                context.content()
        );
    }

    private void validateRequest(
            String question,
            EmbeddingModel embeddingModel,
            int topK
    ) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException(
                    "question must not be blank"
            );
        }

        if (embeddingModel == null) {
            throw new IllegalArgumentException(
                    "embeddingModel must not be null"
            );
        }

        if (!embeddingModel.isAllowedForNewRequests()) {
            throw new IllegalArgumentException(
                    "Embedding model "
                            + embeddingModel
                            + " is not allowed for new requests"
            );
        }

        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException(
                    "topK must be between 1 and 20"
            );
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }

    private long elapsedMillis(long startedAtNanos) {
        return Math.max(
                0L,
                (System.nanoTime() - startedAtNanos) / 1_000_000L
        );
    }

    public record RetrievalDebugResponse(
            String question,
            EmbeddingModel embeddingModel,
            ChunkingStrategy chunkingStrategy,
            int requestedTopK,
            String courseCode,
            String chapterCode,
            long embeddingLatencyMs,
            long searchLatencyMs,
            int resultCount,
            List<RetrievalDebugItem> results
    ) {
    }

    public record RetrievalDebugItem(
            int rank,
            UUID chunkId,
            UUID documentId,
            String sourceFileName,
            String courseCode,
            String chapterCode,
            Integer pageNumber,
            Integer pageStart,
            Integer pageEnd,
            String section,
            Double score,
            String content
    ) {
    }
}