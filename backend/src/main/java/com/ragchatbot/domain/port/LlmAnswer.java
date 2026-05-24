package com.ragchatbot.domain.port;

import java.util.List;

public record LlmAnswer(String answer, List<String> citations, boolean groundedInDocuments) {
}
