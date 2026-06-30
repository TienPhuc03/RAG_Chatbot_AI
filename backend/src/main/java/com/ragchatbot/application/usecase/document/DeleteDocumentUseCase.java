package com.ragchatbot.application.usecase.document;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.ChunkRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;

/*
 * Use case xóa tài liệu khỏi hệ thống. 
 * Xóa theo đúng thứ tự để tránh lỗi foreign key:
 *   1. Xóa vector trong Qdrant
 *   2. Xóa chunk trong Postgres (cascade tự động xóa citation liên quan)
 *   3. Xóa document trong Postgres
 */
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

    /*
     * Xóa document và toàn bộ dữ liệu liên quan (chunks, vectors).
     *
     * @param documentId UUID của document cần xóa
     * @throws RuntimeException nếu document không tồn tại
     */
    public void execute(UUID documentId) {

        // Kiểm tra document tồn tại trước khi xóa
        if (!documentRepository.existsById(documentId)) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        //1. Xóa vector khỏi Qdrant trước (tránh orphan vector nếu DB xóa thất bại)
        vectorStoreService.deleteByDocumentId(documentId);

        //2. Xóa document — DB có ON DELETE CASCADE nên chunks và citations tự xóa theo
        documentRepository.deleteById(documentId);
    }
}