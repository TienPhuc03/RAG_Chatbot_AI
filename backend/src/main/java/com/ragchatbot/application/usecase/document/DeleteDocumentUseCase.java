package com.ragchatbot.application.usecase.document;

import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeleteDocumentUseCase {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final VectorStoreService vectorStoreService;

    public DeleteDocumentUseCase(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            VectorStoreService vectorStoreService
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorStoreService = vectorStoreService;
    }

    public void execute(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        List<EmbeddingModel> embeddingModels = chunkRepository.findDistinctEmbeddingModelsByDocumentId(documentId);
        for (EmbeddingModel embeddingModel : embeddingModels) {
            vectorStoreService.deleteByDocumentId(documentId, embeddingModel);
        }

        documentRepository.delete(document);
    }
}
