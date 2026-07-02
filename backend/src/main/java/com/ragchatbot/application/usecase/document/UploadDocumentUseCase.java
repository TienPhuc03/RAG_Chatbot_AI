package com.ragchatbot.application.usecase.document;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ragchatbot.application.dto.document.DocumentUploadResponse;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;

@Service
public class UploadDocumentUseCase {

    private final DocumentIndexingWorker documentIndexingWorker;
    private final DocumentRepository documentRepository;

    public UploadDocumentUseCase(
            DocumentIndexingWorker documentIndexingWorker,
            DocumentRepository documentRepository
    ) {
        this.documentIndexingWorker = documentIndexingWorker;
        this.documentRepository = documentRepository;
    }

    public DocumentUploadResponse execute(
            MultipartFile file,
            String courseCode,
            String courseName,
            String chapterCode,
            String chapterTitle,
            ChunkingStrategy chunkingStrategy
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("courseCode must not be blank");
        }
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("courseName must not be blank");
        }

        String originalFileName = normalizeFileName(file.getOriginalFilename());
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read uploaded file", ex);
        }

        String checksum = sha256Hex(content);

        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle(stripExtension(originalFileName));
        document.setSourceFileName(originalFileName);
        document.setContentType(contentType);
        document.setChecksum(checksum);
        document.setCourseCode(courseCode);
        document.setCourseName(courseName);
        document.setChapterCode(blankToNull(chapterCode));
        document.setChapterTitle(blankToNull(chapterTitle));
        document.setStatus(DocumentStatus.PENDING);

        documentRepository.saveAndFlush(document);

        documentIndexingWorker.process(new DocumentUploadJob(
                document.getId(),
                content,
                originalFileName,
                contentType,
                courseCode,
                courseName,
                blankToNull(chapterCode),
                blankToNull(chapterTitle),
                null,
                checksum,
                chunkingStrategy
        ));

        return toResponse(document);
    }

    private DocumentUploadResponse toResponse(Document document) {
        return new DocumentUploadResponse(
                document.getId(),
                document.getTitle(),
                document.getSourceFileName(),
                document.getCourseCode(),
                document.getStatus() == null ? DocumentStatus.PENDING : document.getStatus()
        );
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload.bin";
        }
        return fileName.trim();
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
