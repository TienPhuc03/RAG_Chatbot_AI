package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.application.dto.chat.ChatAttachmentUploadResponse;
import com.ragchatbot.application.usecase.document.DocumentIndexingWorker;
import com.ragchatbot.application.usecase.document.DocumentUploadJob;
import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadChatAttachmentUseCase {

    private static final String CHAT_ATTACHMENT_COURSE_CODE = "CHAT_ATTACHMENT";
    private static final String CHAT_ATTACHMENT_COURSE_NAME = "Chat Attachment";
    private static final ChunkingStrategy DEFAULT_CHAT_CHUNKING_STRATEGY = ChunkingStrategy.SEMANTIC;

    private final ConversationRepository conversationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentIndexingWorker documentIndexingWorker;

    public UploadChatAttachmentUseCase(
            ConversationRepository conversationRepository,
            DocumentRepository documentRepository,
            DocumentIndexingWorker documentIndexingWorker
    ) {
        this.conversationRepository = conversationRepository;
        this.documentRepository = documentRepository;
        this.documentIndexingWorker = documentIndexingWorker;
    }

    public ChatAttachmentUploadResponse execute(String sessionId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        Conversation conversation = resolveConversation(sessionId);
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
        document.setCourseCode(CHAT_ATTACHMENT_COURSE_CODE);
        document.setCourseName(CHAT_ATTACHMENT_COURSE_NAME);
        document.setConversationSessionId(conversation.getSessionId());
        document.setStatus(DocumentStatus.PENDING);

        documentRepository.saveAndFlush(document);

        documentIndexingWorker.process(new DocumentUploadJob(
                document.getId(),
                content,
                originalFileName,
                contentType,
                CHAT_ATTACHMENT_COURSE_CODE,
                CHAT_ATTACHMENT_COURSE_NAME,
                null,
                null,
                conversation.getSessionId(),
                checksum,
                DEFAULT_CHAT_CHUNKING_STRATEGY
        ));

        return new ChatAttachmentUploadResponse(
                conversation.getSessionId(),
                document.getId(),
                document.getSourceFileName(),
                document.getStatus(),
                document.getFailureReason()
        );
    }

    private Conversation resolveConversation(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return conversationRepository.findBySessionId(sessionId.trim())
                    .orElseGet(() -> createConversation(sessionId.trim()));
        }

        return createConversation(UUID.randomUUID().toString());
    }

    private Conversation createConversation(String sessionId) {
        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setTitle(null);
        return conversationRepository.saveAndFlush(conversation);
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
