package com.ragchatbot.domain.port;

import java.util.List;

public interface LlmInferenceService {

    LlmAnswer generateAnswer(
            String question,
            List<ConversationTurn> conversationHistory,
            List<RetrievedContext> retrievedContexts
    );
}
