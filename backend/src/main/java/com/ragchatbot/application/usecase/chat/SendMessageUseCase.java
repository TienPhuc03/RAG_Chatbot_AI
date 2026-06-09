package com.ragchatbot.application.usecase.chat;

import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.domain.port.EmbeddingService;
import com.ragchatbot.domain.port.LlmInferenceService;
import com.ragchatbot.domain.port.VectorStoreService;
import com.ragchatbot.infrastructure.persistence.ConversationRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import org.springframework.stereotype.Service;

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

        // TODO:
        // 1. Load conversation
        // 2. Embed question
        // 3. Retrieve context
        // 4. Generate answer
        // 5. Save messages

        return new ChatResponse(
                "Not implemented yet",
                false
        );
    }
}