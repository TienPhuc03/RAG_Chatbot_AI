package com.ragchatbot.domain.port;

import com.ragchatbot.domain.enums.EmbeddingModel;
import java.util.List;

public interface EmbeddingService {

    EmbeddingModel supportedModel();

    List<Float> embed(String text);

    List<List<Float>> embedAll(List<String> texts);
}
