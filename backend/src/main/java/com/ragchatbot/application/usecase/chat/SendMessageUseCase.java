package com.ragchatbot.application.usecase.chat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.domain.enums.MessageRole;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.domain.model.Message;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.RetrievedContext;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SendMessageUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendMessageUseCase.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService llmInferenceService;
    private final ObjectMapper objectMapper;

    public SendMessageUseCase(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            LlmInferenceService llmInferenceService,
            ObjectMapper objectMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.llmInferenceService = llmInferenceService;
        this.objectMapper = objectMapper;
    }

    public ChatResponse execute(ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new IllegalArgumentException("Question must not be blank");
        }

        Conversation conversation = resolveConversation(request);

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

        List<Float> questionEmbedding = embeddingService.embed(request.question());

        List<RetrievedContext> retrievedContexts = vectorStoreService.search(
                questionEmbedding,
                5,
                blankToNull(request.courseCode()),
                blankToNull(request.chapterCode())
        );

        LlmAnswer answer = llmInferenceService.generateAnswer(
                request.question(),
                conversationHistory,
                retrievedContexts
        );

        String citationPayload = serializeCitations(answer.citations());

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
                answer.groundedInDocuments()
        );
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

    private String serializeCitations(List<String> citations) {
        try {
            return objectMapper.writeValueAsString(citations == null ? List.of() : citations);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize citation payload", ex);
            return "[]";
        }
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
}
