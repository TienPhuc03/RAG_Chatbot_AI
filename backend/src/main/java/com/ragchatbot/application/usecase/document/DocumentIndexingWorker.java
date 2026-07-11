package com.ragchatbot.application.usecase.document;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.chunking.ChunkingServiceFactory;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;

@Service
public class DocumentIndexingWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingWorker.class);
    private static final ChunkingOptions DEFAULT_OPTIONS = new ChunkingOptions(512, 50);
    private static final ChunkingStrategy DEFAULT_STRATEGY = ChunkingStrategy.SEMANTIC;

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
    public void process(DocumentUploadJob job) {
        boolean vectorsUpserted = false;
        EmbeddingModel embeddingModel = job.embeddingModel() == null
                ? embeddingProperties.getDefaultModel()
                : job.embeddingModel();
        try {
            Document document = documentRepository.findById(job.documentId())
                    .orElseThrow(() -> new IllegalStateException("Document not found: " + job.documentId()));

            document.setStatus(DocumentStatus.PROCESSING);
            document.setFailureReason(null);
            documentRepository.saveAndFlush(document);

            ParsedDocument parsedDocument = documentParserService.parse(
                    job.content(),
                    job.originalFileName(),
                    job.contentType()
            );

            document.setTitle(parsedDocument.title());
            document.setSourceFileName(job.originalFileName());
            document.setContentType(job.contentType());
            document.setCourseCode(job.courseCode());
            document.setCourseName(job.courseName());
            document.setChapterCode(job.chapterCode());
            document.setChapterTitle(job.chapterTitle());
            document.setConversationSessionId(job.conversationSessionId());
            documentRepository.saveAndFlush(document);

            // SAU (đọc động từ job):
            ChunkingStrategy chunkingStrategy = resolveChunkingStrategy(job);
            log.info("Indexing document {} with chunking strategy {}", job.documentId(), chunkingStrategy);

            List<ChunkDraft> drafts = chunkingServiceFactory.chunk(
                parsedDocument.rawText(),
                chunkingStrategy,
                DEFAULT_OPTIONS

            );
            if (drafts.isEmpty()) {
                throw new IllegalStateException("No chunks generated for document: " + job.documentId());
            }

            List<String> chunkTexts = drafts.stream()
                    .map(ChunkDraft::content)
                    .toList();
            List<List<Float>> embeddings = embeddingRouter.embedAll(embeddingModel, chunkTexts);

            vectorStoreService.upsert(document.getId(), embeddingModel, chunkingStrategy, drafts, embeddings);
            vectorsUpserted = true;
            List<Chunk> chunks = new ArrayList<>(drafts.size());
            for (ChunkDraft draft : drafts) {
                UUID chunkId = chunkId(document.getId(), embeddingModel, chunkingStrategy, draft.chunkIndex());

                Chunk chunk = new Chunk();
                chunk.setId(chunkId);
                chunk.setDocument(document);
                chunk.setChunkIndex(draft.chunkIndex());
                chunk.setContent(draft.content());
                chunk.setPageNumber(draft.pageNumber());
                chunk.setTokenCount(draft.tokenCount());
                chunk.setChunkingStrategy(chunkingStrategy);
                chunk.setEmbeddingModel(embeddingModel);
                chunk.setVectorPointId(chunkId.toString());
                chunks.add(chunk);
            }

            chunkRepository.saveAllAndFlush(chunks);

            document.setIndexedAt(Instant.now());
            document.setStatus(DocumentStatus.INDEXED);
            document.setFailureReason(null);
            documentRepository.saveAndFlush(document);
        } catch (Exception ex) {
            log.error("Failed to index document {}", job.documentId(), ex);
            handleFailure(job.documentId(), embeddingModel, ex.getMessage(), vectorsUpserted);
        }
    }

    private ChunkingStrategy resolveChunkingStrategy(DocumentUploadJob job) {
        if (job.chunkingStrategy() == null) {
            log.warn(
                    "Document {} has no chunkingStrategy in job payload, falling back to default {}",
                    job.documentId(),
                    DEFAULT_STRATEGY
        );
            return DEFAULT_STRATEGY;
        }
        return job.chunkingStrategy();
    }

    private void handleFailure(UUID documentId, EmbeddingModel embeddingModel, String failureReason, boolean vectorsUpserted) {
        try {
            documentRepository.findById(documentId).ifPresent(document -> {
                document.setStatus(DocumentStatus.FAILED);
                document.setFailureReason(failureReason);
                documentRepository.saveAndFlush(document);
            });
        } catch (Exception ex) {
            log.warn("Failed to mark document {} as FAILED", documentId, ex);
        }

        if (vectorsUpserted) {
            try {
                vectorStoreService.deleteByDocumentId(documentId, embeddingModel);
            } catch (Exception ex) {
                log.warn("Failed to clean Qdrant vectors for document {}", documentId, ex);
            }
        }
    }

    private UUID chunkId(
        UUID documentId,
        EmbeddingModel embeddingModel,
        ChunkingStrategy chunkingStrategy,
        int chunkIndex
    ) {
    String raw = documentId + ":" + embeddingModel.name() + ":"
                + chunkingStrategy.name() + ":" + chunkIndex;
    return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8));
    }
}
