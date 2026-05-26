package com.ragchatbot.infrastructure.gemini;

import java.util.List;

public interface GeminiApiClient {

    String generateContent(String modelName, String prompt);

    List<Float> embedContent(String modelName, String text);

    List<List<Float>> embedContents(String modelName, List<String> texts);
}
