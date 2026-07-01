package com.ragchatbot.application.usecase.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragchatbot.application.dto.chat.ChatRequest;
import com.ragchatbot.application.dto.chat.ChatResponse;
import com.ragchatbot.application.usecase.document.DocumentMaintenanceService;
import com.ragchatbot.domain.enums.DocumentStatus;
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
import com.ragchatbot.infrastructure.persistence.DocumentRepository;
import com.ragchatbot.infrastructure.persistence.MessageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SendMessageUseCaseTest {

    @Test
    void executeCreatesConversationSearchesKnowledgeAndStoresCitationPayload() {
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentMaintenanceService documentMaintenanceService = mock(DocumentMaintenanceService.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        LlmInferenceService llmInferenceService = mock(LlmInferenceService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        when(conversationRepository.findBySessionId("session-1")).thenReturn(Optional.empty());
        when(conversationRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Conversation savedConversation = invocation.getArgument(0);
            if (savedConversation.getId() == null) {
                savedConversation.setId(UUID.randomUUID());
            }
            return savedConversation;
        });
        when(messageRepository.countByConversationId(any())).thenReturn(0L);
        when(messageRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Message savedMessage = invocation.getArgument(0);
            if (savedMessage.getId() == null) {
                savedMessage.setId(UUID.randomUUID());
            }
            return savedMessage;
        });

        Message previousAssistant = new Message();
        previousAssistant.setId(UUID.randomUUID());
        previousAssistant.setSequenceNo(2);
        previousAssistant.setRole(MessageRole.ASSISTANT);
        previousAssistant.setContent("Before");
        Message currentUser = new Message();
        currentUser.setId(UUID.randomUUID());
        currentUser.setSequenceNo(1);
        currentUser.setRole(MessageRole.USER);
        currentUser.setContent("Previous question");
        when(messageRepository.findTop5ByConversationIdOrderBySequenceNoDesc(any()))
                .thenReturn(List.of(previousAssistant, currentUser));

        when(embeddingService.embed("Can nang Java la gi?"))
                .thenReturn(List.of(0.1f, 0.2f, 0.3f));
        when(documentRepository.countByStatusAndCourseCodeAndChapterCode(
                DocumentStatus.INDEXED,
                "JAVA101",
                "CH1"
        )).thenReturn(1L);

        RetrievedContext context = new RetrievedContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Java la ngon ngu lap trinh.",
                0.91,
                "JAVA101",
                "CH1"
        );
        when(vectorStoreService.search(eq(List.of(0.1f, 0.2f, 0.3f)), eq(5), eq("JAVA101"), eq("CH1")))
                .thenReturn(List.of(context));

        when(llmInferenceService.generateAnswer(
                eq("Can nang Java la gi?"),
                any(),
                eq(List.of(context))
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ConversationTurn> history = invocation.getArgument(1);
            assertThat(history).extracting(ConversationTurn::role)
                    .containsExactly(MessageRole.USER, MessageRole.ASSISTANT);
            assertThat(history).extracting(ConversationTurn::content)
                    .containsExactly("Previous question", "Before");
            return new LlmAnswer("Java co the hoi, nhung rat manh.", List.of("doc-1:chunk-1"), true);
        });

        SendMessageUseCase useCase = new SendMessageUseCase(
                conversationRepository,
                messageRepository,
                documentRepository,
                documentMaintenanceService,
                embeddingService,
                vectorStoreService,
                llmInferenceService,
                objectMapper
        );

        ChatResponse response = useCase.execute(new ChatRequest(
                "session-1",
                "Can nang Java la gi?",
                "JAVA101",
                "CH1"
        ));

        assertThat(response.answer()).isEqualTo("Java co the hoi, nhung rat manh.");
        assertThat(response.groundedInDocuments()).isTrue();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, org.mockito.Mockito.times(2)).saveAndFlush(messageCaptor.capture());
        List<Message> savedMessages = messageCaptor.getAllValues();
        assertThat(savedMessages).hasSize(2);
        assertThat(savedMessages.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(savedMessages.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(savedMessages.get(1).getCitationPayload()).isEqualTo("[\"doc-1:chunk-1\"]");
        verify(conversationRepository, org.mockito.Mockito.atLeast(2)).saveAndFlush(any());
    }
}
