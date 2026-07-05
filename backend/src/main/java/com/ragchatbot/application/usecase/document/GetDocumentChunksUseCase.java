package com.ragchatbot.application.usecase.document;

import com.ragchatbot.application.dto.document.DocumentChunkPreviewResponse;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetDocumentChunksUseCase {

    private final ChunkRepository chunkRepository;

    public GetDocumentChunksUseCase(ChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public List<DocumentChunkPreviewResponse> execute(UUID documentId) {
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .limit(5)
                .map(chunk -> new DocumentChunkPreviewResponse(
                        chunk.getChunkIndex(),
                        chunk.getPageNumber(),
                        chunk.getTokenCount(),
                        chunk.getChunkingStrategy(),
                        chunk.getEmbeddingModel(),
                        chunk.getContent()
                ))
                .toList();
    }
}
