package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import org.springframework.stereotype.Service;

import com.ragchatbot.domain.enums.MessageRole;
import com.ragchatbot.domain.model.Conversation;
import com.ragchatbot.domain.model.Message;
import com.ragchatbot.domain.port.ConversationTurn;
import com.ragchatbot.domain.port.LlmAnswer;
import com.ragchatbot.domain.port.RetrievedContext;

import java.util.List;
import java.util.UUID;

@Service
public class SendMessageUseCase {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final LlmInferenceService llmInferenceService;

    public SendMessageUseCase(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            EmbeddingService embeddingService,
            VectorStoreService vectorStoreService,
            LlmInferenceService llmInferenceService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.llmInferenceService = llmInferenceService;
    }

    /**
     * Xử lý câu hỏi và sinh câu trả lời từ hệ thống RAG.
     */
    public ChatResponse execute(ChatRequest request) {

        Conversation conversation = resolveConversation(request);

        List<Message> recentMessages =
                messageRepository.findTop5ByConversationIdOrderBySequenceNoDesc(conversation.getId());

        List<ConversationTurn> conversationHistory = recentMessages.stream()
                .sorted((a, b) -> a.getSequenceNo().compareTo(b.getSequenceNo()))
                .map(message -> new ConversationTurn(
                        message.getRole(),
                        message.getContent()
                ))
                .toList();

        long currentMessageCount =
                messageRepository.countByConversationId(conversation.getId());

        int userSequenceNo = (int) currentMessageCount + 1;
        int assistantSequenceNo = userSequenceNo + 1;

        saveMessage(conversation, userSequenceNo, MessageRole.USER, request.question());

        List<Float> queryEmbedding = embeddingService.embed(request.question());

        List<RetrievedContext> retrievedContexts = vectorStoreService.search(
                queryEmbedding,
                5,
                request.courseCode(),
                request.chapterCode()
        );

        LlmAnswer llmAnswer = llmInferenceService.generateAnswer(
                request.question(),
                conversationHistory,
                retrievedContexts
        );

        saveMessage(
                conversation,
                assistantSequenceNo,
                MessageRole.ASSISTANT,
                llmAnswer.answer()
        );

        return new ChatResponse(
                conversation.getId().toString(),
                conversation.getSessionId(),
                llmAnswer.answer(),
                llmAnswer.groundedInDocuments()
        );
    }


    /**
     * Tìm Conversation theo sessionId.
     * Nếu chưa tồn tại thì tạo Conversation mới.
     */
    private Conversation resolveConversation(ChatRequest request) {

        String sessionId = request.sessionId();

        if (sessionId != null && !sessionId.isBlank()) {
            return conversationRepository.findBySessionId(sessionId)
                    .orElseGet(() -> createConversation(sessionId, request.question()));
        }

        return createConversation(UUID.randomUUID().toString(), request.question());
    }

    private Conversation createConversation(String sessionId, String question) {

        Conversation conversation = new Conversation();
        conversation.setSessionId(sessionId);
        conversation.setTitle(buildTitle(question));

        return conversationRepository.save(conversation);
    }

    private void saveMessage(
            Conversation conversation,
            int sequenceNo,
            MessageRole role,
            String content
    ) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSequenceNo(sequenceNo);
        message.setRole(role);
        message.setContent(content);

        messageRepository.save(message);
    }

    private String buildTitle(String question) {

        if (question == null || question.isBlank()) {
            return "New conversation";
        }

        String normalized = question.trim();

        if (normalized.length() <= 80) {
            return normalized;
        }

        return normalized.substring(0, 80);
    }
}