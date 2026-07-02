package com.ragchatbot.application.usecase.document;

import com.ragchatbot.application.dto.document.DocumentListResponse;
import com.ragchatbot.domain.model.Chunk;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GetDocumentsUseCase {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final DocumentMaintenanceService documentMaintenanceService;

    public GetDocumentsUseCase(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            DocumentMaintenanceService documentMaintenanceService
    ) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.documentMaintenanceService = documentMaintenanceService;
    }

    public List<Document> getEntities(String courseCode) {
        if (!StringUtils.hasText(courseCode)) {
            return documentRepository.findAllByConversationSessionIdIsNullOrderByCreatedAtDesc();
        }
        return documentRepository.findByCourseCodeAndConversationSessionIdIsNullOrderByCreatedAtDesc(courseCode);
    }

    public List<DocumentListResponse> execute(String courseCode) {
        documentMaintenanceService.reconcileStaleProcessingDocuments();
        return getEntities(courseCode).stream()
                .map(this::toResponse)
                .toList();
    }

    private DocumentListResponse toResponse(Document document) {
        long chunkCount = chunkRepository.countByDocumentId(document.getId());
        Chunk firstChunk = chunkRepository.findTopByDocumentIdOrderByChunkIndexAsc(document.getId()).orElse(null);

        return new DocumentListResponse(
                document.getId(),
                document.getTitle(),
                document.getSourceFileName(),
                document.getCourseCode(),
                document.getCourseName(),
                document.getChapterCode(),
                document.getChapterTitle(),
                document.getStatus(),
                document.getFailureReason(),
                chunkCount,
                firstChunk == null ? null : firstChunk.getChunkingStrategy(),
                document.getIndexedAt(),
                document.getCreatedAt()
        );
    }
}
