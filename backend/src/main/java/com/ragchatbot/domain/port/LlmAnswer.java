package com.ragchatbot.domain.port;

import java.util.List;

public record LlmAnswer(String answer, List<CitationReference> citations, boolean groundedInDocuments) {
}
