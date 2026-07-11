package com.ragchatbot.application.usecase.chat;
import com.ragchatbot.config.EmbeddingProperties;
import com.ragchatbot.application.dto.chat.ChatCitationDto;
import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.enums.DocumentStatus;
import com.ragchatbot.domain.enums.EmbeddingModel;
import com.ragchatbot.domain.enums.MessageRole;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.domain.model.Document;
import com.ragchatbot.domain.model.Message;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.embedding.EmbeddingRouter;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SendMessageUseCase {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final DocumentRepository documentRepository;
    private final DocumentMaintenanceService documentMaintenanceService;
    private final EmbeddingRouter embeddingRouter;
    private final EmbeddingProperties embeddingProperties;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService llmInferenceService;
    private final ChatCitationPayloadCodec citationPayloadCodec;

    public SendMessageUseCase(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            DocumentRepository documentRepository,
            DocumentMaintenanceService documentMaintenanceService,
            EmbeddingRouter embeddingRouter,
            EmbeddingProperties embeddingProperties,
            VectorStoreService vectorStoreService,
            LlmInferenceService llmInferenceService,
            ChatCitationPayloadCodec citationPayloadCodec
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.documentRepository = documentRepository;
        this.documentMaintenanceService = documentMaintenanceService;
        this.embeddingRouter = embeddingRouter;
        this.embeddingProperties = embeddingProperties;
        this.vectorStoreService = vectorStoreService;
        this.llmInferenceService = llmInferenceService;
        this.citationPayloadCodec = citationPayloadCodec;
    }

    public ChatResponse execute(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new IllegalArgumentException("Question must not be blank");
        }

        Conversation conversation = resolveConversation(request);
        documentMaintenanceService.reconcileStaleProcessingDocuments();

        if (!StringUtils.hasText(conversation.getTitle())) {
            conversation.setTitle(deriveTitle(request.question()));
            conversationRepository.saveAndFlush(conversation);
        }

        long existingMessages = messageRepository.countByConversationId(conversation.getId());

        List<Message> recentMessages = new ArrayList<>(
                messageRepository.findTop5ByConversationIdOrderBySequenceNoDesc(conversation.getId())
        );
        Collections.reverse(recentMessages);

        List<ConversationTurn> conversationHistory = recentMessages.stream()
                .map(message -> new ConversationTurn(
                        message.getRole(),
                        message.getContent()
                ))
                .toList();

        int userSequenceNo = Math.toIntExact(existingMessages + 1);
        int assistantSequenceNo = userSequenceNo + 1;

        saveMessage(
                conversation,
                userSequenceNo,
                MessageRole.USER,
                request.question(),
                null
        );

        String conversationSessionId = conversation.getSessionId();
        String courseCode = blankToNull(request.courseCode());
        String chapterCode = blankToNull(request.chapterCode());
        List<Document> conversationAttachments = documentRepository.findByConversationSessionIdOrderByCreatedAtAsc(
                conversationSessionId
        );

        AttachmentResolution attachmentResolution = resolveAttachmentState(conversationAttachments);
        if (attachmentResolution.shouldStop()) {
            return buildAssistantOnlyResponse(
                    conversation,
                    assistantSequenceNo,
                    attachmentResolution.message()
            );
        }

        boolean useConversationAttachments = attachmentResolution.useIndexedAttachments();
        if (!useConversationAttachments && !hasIndexedDocuments(courseCode, chapterCode)) {
            return buildAssistantOnlyResponse(
                    conversation,
                    assistantSequenceNo,
                    buildUnavailableDocumentMessage(courseCode, chapterCode)
            );
        }

        EmbeddingModel embeddingModel = resolveEmbeddingModel(request.embeddingModel());
        List<Float> questionEmbedding = embeddingRouter.embed(embeddingModel, request.question());

        List<RetrievedContext> retrievedContexts = vectorStoreService.search(
                embeddingModel,
                questionEmbedding,
                5,
                null,
                useConversationAttachments ? null : courseCode,
                useConversationAttachments ? null : chapterCode,
                useConversationAttachments ? conversationSessionId : null
        );

        if (retrievedContexts.isEmpty()) {
            return buildAssistantOnlyResponse(
                    conversation,
                    assistantSequenceNo,
                    buildNoContextMessage(embeddingModel, useConversationAttachments)
            );
        }

        LlmAnswer answer = llmInferenceService.generateAnswer(
                request.question(),
                conversationHistory,
                retrievedContexts
        );

        List<ChatCitationDto> citations = citationPayloadCodec.toDtos(answer.citations());
        String citationPayload = citationPayloadCodec.serialize(citations);

        saveMessage(
                conversation,
                assistantSequenceNo,
                MessageRole.ASSISTANT,
                answer.answer(),
                citationPayload
        );

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.saveAndFlush(conversation);

        return new ChatResponse(
                conversation.getId().toString(),
                conversation.getSessionId(),
                answer.answer(),
                answer.groundedInDocuments(),
                citations
        );
    }

    private EmbeddingModel resolveEmbeddingModel(EmbeddingModel requestedModel) {
        if (requestedModel == null) {
            return embeddingProperties.getDefaultModel();
        }

        if (!requestedModel.isAllowedForNewRequests()) {
            throw new IllegalArgumentException(
                    "Embedding model " + requestedModel + " da ngung ho tro cho request moi."
            );
        }

        return requestedModel;
    }

    private boolean hasIndexedDocuments(String courseCode, String chapterCode) {
        return documentMaintenanceService.hasIndexedDocuments(courseCode, chapterCode);
    }

    private ChatResponse buildAssistantOnlyResponse(
            Conversation conversation,
            int assistantSequenceNo,
            String answer
    ) {
        saveMessage(conversation, assistantSequenceNo, MessageRole.ASSISTANT, answer, "[]");
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.saveAndFlush(conversation);
        return new ChatResponse(
                conversation.getId().toString(),
                conversation.getSessionId(),
                answer,
                false,
                List.of()
        );
    }

    private AttachmentResolution resolveAttachmentState(List<Document> conversationAttachments) {
        if (conversationAttachments == null || conversationAttachments.isEmpty()) {
            return AttachmentResolution.withoutAttachment();
        }

        boolean hasProcessingAttachment = conversationAttachments.stream()
                .map(Document::getStatus)
                .anyMatch(status -> status == DocumentStatus.PENDING || status == DocumentStatus.PROCESSING);
        if (hasProcessingAttachment) {
            return AttachmentResolution.stop(
                    "File dang duoc xu ly. Vui long doi index xong roi gui cau hoi lai."
            );
        }

        List<Document> indexedAttachments = conversationAttachments.stream()
                .filter(document -> document.getStatus() == DocumentStatus.INDEXED)
                .toList();
        if (!indexedAttachments.isEmpty()) {
            return AttachmentResolution.withIndexedAttachments();
        }

        Document latestFailedAttachment = conversationAttachments.get(conversationAttachments.size() - 1);
        String failureReason = latestFailedAttachment.getFailureReason();
        if (StringUtils.hasText(failureReason)) {
            return AttachmentResolution.stop("File da gan bi loi: " + failureReason);
        }

        return AttachmentResolution.stop(
                "File da gan chua san sang de tra loi. Hay thu upload lai bang DOCX hoac PDF co the copy text."
        );
    }

    private String buildUnavailableDocumentMessage(String courseCode, String chapterCode) {
        if (courseCode != null && chapterCode != null) {
            return "Chua co tai lieu nao o trang thai INDEXED cho courseCode "
                    + courseCode
                    + " va chapterCode "
                    + chapterCode
                    + ". Hay upload file DOCX hoac PDF co the copy text, roi doi index xong.";
        }
        if (courseCode != null) {
            return "Chua co tai lieu nao o trang thai INDEXED cho courseCode "
                    + courseCode
                    + ". Hay upload file DOCX hoac PDF co the copy text, roi doi index xong.";
        }
        return "He thong chua co tai lieu nao o trang thai INDEXED. Hay upload file DOCX hoac PDF co the copy text truoc khi chat.";
    }

    private String buildNoContextMessage(EmbeddingModel embeddingModel, boolean useConversationAttachments) {
        if (useConversationAttachments) {
            return "Khong tim thay ngu canh phu hop trong file da gan voi embedding model "
                    + embeddingModel
                    + ". Hay hoi cu the hon hoac thu lai dung embedding model khi upload file.";
        }

        return "Khong tim thay ngu canh phu hop voi embedding model "
                + embeddingModel
                + ". Hay kiem tra tai lieu da upload bang cung embedding model nay.";
    }

    private Conversation resolveConversation(ChatRequest request) {
        String sessionId = request.sessionId();

        if (StringUtils.hasText(sessionId)) {
            return conversationRepository.findBySessionId(sessionId.trim())
                    .orElseGet(() -> createConversation(sessionId.trim(), request.question()));
        }

        return createConversation(UUID.randomUUID().toString(), request.question());
    }

    private Conversation createConversation(String sessionId, String firstQuestion) {
        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setTitle(deriveTitle(firstQuestion));
        return conversationRepository.saveAndFlush(conversation);
    }

    private Message saveMessage(
            Conversation conversation,
            long sequenceNo,
            MessageRole role,
            String content,
            String citationPayload
    ) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSequenceNo(Math.toIntExact(sequenceNo));
        message.setRole(role);
        message.setContent(content);
        message.setCitationPayload(citationPayload);
        return messageRepository.saveAndFlush(message);
    }

    private String deriveTitle(String question) {
        if (!StringUtils.hasText(question)) {
            return "Conversation";
        }

        String trimmed = question.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 77) + "...";
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record AttachmentResolution(
            boolean useIndexedAttachments,
            boolean shouldStop,
            String message
    ) {
        private static AttachmentResolution withoutAttachment() {
            return new AttachmentResolution(false, false, null);
        }

        private static AttachmentResolution withIndexedAttachments() {
            return new AttachmentResolution(true, false, null);
        }

        private static AttachmentResolution stop(String message) {
            return new AttachmentResolution(false, true, message);
        }
    }
}
