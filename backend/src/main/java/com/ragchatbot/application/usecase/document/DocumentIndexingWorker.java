package com.ragchatbot.application.usecase.document;

import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.model.Chunk;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.DocumentParserService;
import com.ragchatbot.domain.port.ParsedDocument;
import com.ragchatbot.domain.port.ParsedPage;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.chunking.ChunkingServiceFactory;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentIndexingWorker {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DocumentIndexingWorker.class
            );

    private static final ChunkingOptions DEFAULT_OPTIONS =
            new ChunkingOptions(
                    512,
                    50
            );

    private static final ChunkingStrategy DEFAULT_STRATEGY =
            ChunkingStrategy.SEMANTIC;

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentParserService documentParserService;
    private final ChunkingServiceFactory chunkingServiceFactory;
    private final EmbeddingRouter embeddingRouter;
    private final EmbeddingProperties embeddingProperties;
    private final VectorStoreService vectorStoreService;

    public DocumentIndexingWorker(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            DocumentParserService documentParserService,
            ChunkingServiceFactory chunkingServiceFactory,
            EmbeddingRouter embeddingRouter,
            EmbeddingProperties embeddingProperties,
            VectorStoreService vectorStoreService
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentParserService = documentParserService;
        this.chunkingServiceFactory = chunkingServiceFactory;
        this.embeddingRouter = embeddingRouter;
        this.embeddingProperties = embeddingProperties;
        this.vectorStoreService = vectorStoreService;
    }

    @Async("applicationTaskExecutor")
    public void process(
            DocumentUploadJob job
    ) {
        boolean vectorsUpserted = false;

        EmbeddingModel embeddingModel =
                job.embeddingModel() == null
                        ? embeddingProperties
                        .getDefaultModel()
                        : job.embeddingModel();

        try {
            Document document =
                    documentRepository
                            .findById(
                                    job.documentId()
                            )
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Document not found: "
                                                            + job.documentId()
                                            )
                            );

            document.setStatus(
                    DocumentStatus.PROCESSING
            );

            document.setFailureReason(null);

            documentRepository.saveAndFlush(
                    document
            );

            ParsedDocument parsedDocument =
                    documentParserService.parse(
                            job.content(),
                            job.originalFileName(),
                            job.contentType()
                    );

            document.setTitle(
                    parsedDocument.title()
            );

            document.setSourceFileName(
                    job.originalFileName()
            );

            document.setContentType(
                    job.contentType()
            );

            document.setCourseCode(
                    job.courseCode()
            );

            document.setCourseName(
                    job.courseName()
            );

            document.setChapterCode(
                    job.chapterCode()
            );

            document.setChapterTitle(
                    job.chapterTitle()
            );

            document.setConversationSessionId(
                    job.conversationSessionId()
            );

            documentRepository.saveAndFlush(
                    document
            );

            ChunkingStrategy chunkingStrategy =
                    resolveChunkingStrategy(job);

            log.info(
                    "Indexing document {} with strategy {} "
                            + "and {} parsed pages",
                    job.documentId(),
                    chunkingStrategy,
                    parsedDocument.pages().size()
            );

            /*
             * Thay vì chunk toàn bộ rawText một lần,
             * ta chunk riêng từng trang vật lý.
             */
            List<ChunkDraft> chunkDrafts =
                    chunkParsedPages(
                            parsedDocument,
                            chunkingStrategy,
                            DEFAULT_OPTIONS
                    );

            if (chunkDrafts.isEmpty()) {
                throw new IllegalStateException(
                        "No chunks generated for document: "
                                + job.documentId()
                );
            }

            List<String> chunkTexts =
                    chunkDrafts.stream()
                            .map(
                                    ChunkDraft::content
                            )
                            .toList();

            List<List<Float>> embeddings =
                    embeddingRouter.embedAll(
                            embeddingModel,
                            chunkTexts
                    );

            if (embeddings.size()
                    != chunkDrafts.size()) {

                throw new IllegalStateException(
                        "Embedding count does not match chunk count. "
                                + "chunks="
                                + chunkDrafts.size()
                                + ", embeddings="
                                + embeddings.size()
                );
            }

            vectorStoreService.upsert(
                    document.getId(),
                    embeddingModel,
                    chunkingStrategy,
                    chunkDrafts,
                    embeddings
            );

            vectorsUpserted = true;

            List<Chunk> chunks =
                    new ArrayList<>(
                            chunkDrafts.size()
                    );

            for (ChunkDraft chunkDraft
                    : chunkDrafts) {

                UUID chunkId =
                        createChunkId(
                                document.getId(),
                                embeddingModel,
                                chunkingStrategy,
                                chunkDraft.chunkIndex()
                        );

                Chunk chunk =
                        new Chunk();

                chunk.setId(chunkId);
                chunk.setDocument(document);

                chunk.setChunkIndex(
                        chunkDraft.chunkIndex()
                );

                chunk.setContent(
                        chunkDraft.content()
                );

                chunk.setPageNumber(
                        chunkDraft.pageNumber()
                );

                chunk.setTokenCount(
                        chunkDraft.tokenCount()
                );

                chunk.setChunkingStrategy(
                        chunkingStrategy
                );

                chunk.setEmbeddingModel(
                        embeddingModel
                );

                chunk.setVectorPointId(
                        chunkId.toString()
                );

                chunks.add(chunk);
            }

            chunkRepository.saveAllAndFlush(
                    chunks
            );

            document.setIndexedAt(
                    Instant.now()
            );

            document.setStatus(
                    DocumentStatus.INDEXED
            );

            document.setFailureReason(null);

            documentRepository.saveAndFlush(
                    document
            );

            log.info(
                    "Indexed document {} successfully: "
                            + "{} pages, {} chunks",
                    document.getId(),
                    parsedDocument.pages().size(),
                    chunks.size()
            );

        } catch (Exception exception) {
            log.error(
                    "Failed to index document {}",
                    job.documentId(),
                    exception
            );

            handleFailure(
                    job.documentId(),
                    embeddingModel,
                    exception.getMessage(),
                    vectorsUpserted
            );
        }
    }

    /**
     * Chunk riêng từng trang.
     *
     * Mục tiêu:
     * - mỗi chunk chỉ thuộc một trang;
     * - giữ pageNumber vật lý chính xác;
     * - không tạo chunk xuyên từ trang này sang trang khác;
     * - giữ chunkIndex duy nhất trên toàn tài liệu;
     * - remap parentChunkId cho hierarchical chunking.
     */
    private List<ChunkDraft> chunkParsedPages(
            ParsedDocument parsedDocument,
            ChunkingStrategy chunkingStrategy,
            ChunkingOptions chunkingOptions
    ) {
        List<ParsedPage> parsedPages =
                parsedDocument.pages();

        /*
         * Fallback tương thích cho dữ liệu cũ hoặc định dạng
         * không có thông tin trang.
         */
        if (parsedPages == null
                || parsedPages.isEmpty()) {

            if (parsedDocument.rawText() == null
                    || parsedDocument
                    .rawText()
                    .isBlank()) {

                return List.of();
            }

            parsedPages =
                    List.of(
                            new ParsedPage(
                                    1,
                                    parsedDocument.rawText()
                            )
                    );
        }

        List<ChunkDraft> globalDrafts =
                new ArrayList<>();

        int nextGlobalChunkIndex = 0;

        for (ParsedPage parsedPage
                : parsedPages) {

            /*
             * Trang rỗng vẫn tồn tại trong ParsedDocument
             * để giữ đúng số trang, nhưng không cần tạo chunk.
             */
            if (parsedPage.text() == null
                    || parsedPage
                    .text()
                    .isBlank()) {

                continue;
            }

            List<ChunkDraft> localDrafts =
                    chunkingServiceFactory.chunk(
                            parsedPage.text(),
                            chunkingStrategy,
                            chunkingOptions
                    );

            if (localDrafts.isEmpty()) {
                continue;
            }

            /*
             * Mỗi chunker bắt đầu index từ 0 khi xử lý một trang.
             * Cần ánh xạ local index sang global index.
             */
            Map<Integer, Integer> localToGlobalIndex =
                    new HashMap<>();

            for (ChunkDraft localDraft
                    : localDrafts) {

                localToGlobalIndex.put(
                        localDraft.chunkIndex(),
                        nextGlobalChunkIndex
                );

                nextGlobalChunkIndex++;
            }

            for (ChunkDraft localDraft
                    : localDrafts) {

                Integer globalChunkIndex =
                        localToGlobalIndex.get(
                                localDraft.chunkIndex()
                        );

                if (globalChunkIndex == null) {
                    throw new IllegalStateException(
                            "Không thể ánh xạ chunkIndex "
                                    + localDraft.chunkIndex()
                                    + " tại trang "
                                    + parsedPage.pageNumber()
                    );
                }

                Integer globalParentChunkIndex =
                        null;

                if (localDraft.parentChunkId()
                        != null) {

                    globalParentChunkIndex =
                            localToGlobalIndex.get(
                                    localDraft
                                            .parentChunkId()
                            );

                    if (globalParentChunkIndex
                            == null) {

                        throw new IllegalStateException(
                                "parentChunkId không hợp lệ: "
                                        + localDraft
                                        .parentChunkId()
                                        + " tại trang "
                                        + parsedPage
                                        .pageNumber()
                        );
                    }
                }

                /*
                 * pageNumber được gán tại đây.
                 *
                 * Chunker không tự đoán số trang.
                 */
                globalDrafts.add(
                        new ChunkDraft(
                                globalChunkIndex,
                                localDraft.content(),
                                parsedPage.pageNumber(),
                                localDraft.tokenCount(),
                                globalParentChunkIndex
                        )
                );
            }
        }

        return globalDrafts;
    }

    private ChunkingStrategy resolveChunkingStrategy(
            DocumentUploadJob job
    ) {
        if (job.chunkingStrategy() == null) {
            log.warn(
                    "Document {} has no chunkingStrategy. "
                            + "Falling back to {}",
                    job.documentId(),
                    DEFAULT_STRATEGY
            );

            return DEFAULT_STRATEGY;
        }

        return job.chunkingStrategy();
    }

    private void handleFailure(
            UUID documentId,
            EmbeddingModel embeddingModel,
            String failureReason,
            boolean vectorsUpserted
    ) {
        try {
            documentRepository
                    .findById(documentId)
                    .ifPresent(document -> {
                        document.setStatus(
                                DocumentStatus.FAILED
                        );

                        document.setFailureReason(
                                failureReason
                        );

                        documentRepository
                                .saveAndFlush(
                                        document
                                );
                    });

        } catch (Exception exception) {
            log.warn(
                    "Failed to mark document {} as FAILED",
                    documentId,
                    exception
            );
        }

        /*
         * Nếu đã upsert Qdrant nhưng DB lưu chunk lỗi,
         * xóa vectors để tránh dữ liệu mồ côi.
         */
        if (vectorsUpserted) {
            try {
                vectorStoreService
                        .deleteByDocumentId(
                                documentId,
                                embeddingModel
                        );

            } catch (Exception exception) {
                log.warn(
                        "Failed to clean Qdrant vectors "
                                + "for document {}",
                        documentId,
                        exception
                );
            }
        }
    }

    private UUID createChunkId(
            UUID documentId,
            EmbeddingModel embeddingModel,
            ChunkingStrategy chunkingStrategy,
            int chunkIndex
    ) {
        String rawId =
                documentId
                        + ":"
                        + embeddingModel.name()
                        + ":"
                        + chunkingStrategy.name()
                        + ":"
                        + chunkIndex;

        return UUID.nameUUIDFromBytes(
                rawId.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}