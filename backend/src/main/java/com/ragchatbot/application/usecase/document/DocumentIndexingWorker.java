package com.ragchatbot.application.usecase.document;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.model.Chunk;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.DocumentParserService;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.ParsedDocument;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.chunking.ChunkingServiceFactory;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DocumentIndexingWorker {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingWorker.class);
    private static final ChunkingOptions DEFAULT_OPTIONS = new ChunkingOptions(512, 50);

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentParserService documentParserService;
    private final ChunkingServiceFactory chunkingServiceFactory;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    public DocumentIndexingWorker(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            DocumentParserService documentParserService,
            ChunkingServiceFactory chunkingServiceFactory,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentParserService = documentParserService;
        this.chunkingServiceFactory = chunkingServiceFactory;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    @Async("applicationTaskExecutor")
    public void process(DocumentUploadJob job) {
        boolean vectorsUpserted = false;
        try {
            Document document = documentRepository.findById(job.documentId())
                    .orElseThrow(() -> new IllegalStateException("Document not found: " + job.documentId()));

            document.setStatus(DocumentStatus.PROCESSING);
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
            documentRepository.saveAndFlush(document);

            List<ChunkDraft> drafts = chunkingServiceFactory.chunk(
                    parsedDocument.rawText(),
                    ChunkingStrategy.SEMANTIC,
                    DEFAULT_OPTIONS
            );
            if (drafts.isEmpty()) {
                throw new IllegalStateException("No chunks generated for document: " + job.documentId());
            }

            List<String> chunkTexts = drafts.stream()
                    .map(ChunkDraft::content)
                    .toList();
            List<List<Float>> embeddings = embeddingService.embedAll(chunkTexts);

            vectorStoreService.upsert(document.getId(), drafts, embeddings);
            vectorsUpserted = true;

            EmbeddingModel embeddingModel = embeddingService.supportedModel();
            List<Chunk> chunks = new ArrayList<>(drafts.size());
            for (ChunkDraft draft : drafts) {
                UUID chunkId = chunkId(document.getId(), draft.chunkIndex());

                Chunk chunk = new Chunk();
                chunk.setId(chunkId);
                chunk.setDocument(document);
                chunk.setChunkIndex(draft.chunkIndex());
                chunk.setContent(draft.content());
                chunk.setPageNumber(draft.pageNumber());
                chunk.setTokenCount(draft.tokenCount());
                chunk.setChunkingStrategy(ChunkingStrategy.SEMANTIC);
                chunk.setEmbeddingModel(embeddingModel);
                chunk.setVectorPointId(chunkId.toString());
                chunks.add(chunk);
            }

            chunkRepository.saveAllAndFlush(chunks);

            document.setIndexedAt(Instant.now());
            document.setStatus(DocumentStatus.INDEXED);
            documentRepository.saveAndFlush(document);
        } catch (Exception ex) {
            log.error("Failed to index document {}", job.documentId(), ex);
            handleFailure(job.documentId(), vectorsUpserted);
        }
    }

    private void handleFailure(UUID documentId, boolean vectorsUpserted) {
        try {
            documentRepository.findById(documentId).ifPresent(document -> {
                document.setStatus(DocumentStatus.FAILED);
                documentRepository.saveAndFlush(document);
            });
        } catch (Exception ex) {
            log.warn("Failed to mark document {} as FAILED", documentId, ex);
        }

        if (vectorsUpserted) {
            try {
                vectorStoreService.deleteByDocumentId(documentId);
            } catch (Exception ex) {
                log.warn("Failed to clean Qdrant vectors for document {}", documentId, ex);
            }
        }
    }

    private UUID chunkId(UUID documentId, int chunkIndex) {
        return UUID.nameUUIDFromBytes((documentId + ":" + chunkIndex).getBytes(StandardCharsets.UTF_8));
    }
}
