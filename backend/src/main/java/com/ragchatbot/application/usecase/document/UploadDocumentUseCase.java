package com.ragchatbot.application.usecase.document;

import com.ragchatbot.domain.port.ChunkingService;
import com.ragchatbot.domain.port.DocumentParserService;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.chunking.ChunkingServiceFactory;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadDocumentUseCase {

    private final DocumentParserService documentParserService;
    private final ChunkingServiceFactory chunkingServiceFactory;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;

    public UploadDocumentUseCase(
            DocumentParserService documentParserService,
            ChunkingServiceFactory chunkingServiceFactory,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository
    ) {
        this.documentParserService = documentParserService;
        this.chunkingServiceFactory = chunkingServiceFactory;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Upload và indexing tài liệu.
     */
    public void execute(MultipartFile file) {

        // TODO:
        // 1. Parse document
        // 2. Chunk document
        // 3. Generate embeddings
        // 4. Store vectors
        // 5. Save document/chunks
    }
}