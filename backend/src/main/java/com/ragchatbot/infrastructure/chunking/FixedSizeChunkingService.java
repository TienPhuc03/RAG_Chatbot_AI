package com.ragchatbot.infrastructure.chunking;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.ChunkingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FixedSizeChunkingService
        implements ChunkingService {

    private static final int DEFAULT_CHUNK_SIZE = 512;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    @Override
    public List<ChunkDraft> chunk(
            String rawText,
            ChunkingStrategy strategy,
            ChunkingOptions options
    ) {
        List<ChunkDraft> chunks =
                new ArrayList<>();

        if (rawText == null
                || rawText.trim().isEmpty()) {

            return chunks;
        }

        int chunkSize =
                options != null
                        && options.chunkSize() > 0
                        ? options.chunkSize()
                        : DEFAULT_CHUNK_SIZE;

        int chunkOverlap =
                options != null
                        && options.chunkOverlap() >= 0
                        ? options.chunkOverlap()
                        : DEFAULT_CHUNK_OVERLAP;

        /*
         * Overlap phải nhỏ hơn chunk size để cửa sổ tiến lên.
         */
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "chunkOverlap phải nhỏ hơn chunkSize."
            );
        }

        int start = 0;
        int textLength = rawText.length();
        int currentIndex = 0;

        while (start < textLength) {
            int end =
                    Math.min(
                            start + chunkSize,
                            textLength
                    );

            String chunkContent =
                    rawText.substring(
                            start,
                            end
                    );

            int tokenCount =
                    chunkContent.length();

            /*
             * pageNumber = null vì chunker không biết
             * mình đang xử lý trang nào.
             *
             * DocumentIndexingWorker sẽ gán trang thật.
             */
            ChunkDraft chunkDraft =
                    new ChunkDraft(
                            currentIndex,
                            chunkContent,
                            null,
                            tokenCount
                    );

            chunks.add(chunkDraft);
            currentIndex++;

            if (end == textLength) {
                break;
            }

            int nextStart =
                    end - chunkOverlap;

            /*
             * Bảo vệ tránh vòng lặp không tiến lên.
             */
            if (nextStart <= start) {
                nextStart = end;
            }

            start = nextStart;
        }

        return chunks;
    }
}