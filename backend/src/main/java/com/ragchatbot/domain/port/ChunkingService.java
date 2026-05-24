package com.ragchatbot.domain.port;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import java.util.List;

public interface ChunkingService {

    List<ChunkDraft> chunk(String rawText, ChunkingStrategy strategy, ChunkingOptions options);
}
