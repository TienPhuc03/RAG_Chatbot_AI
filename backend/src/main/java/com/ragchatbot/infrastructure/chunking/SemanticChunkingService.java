package com.ragchatbot.infrastructure.chunking;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.ChunkingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SemanticChunkingService
        implements ChunkingService {

    /**
     * Câu dài hơn ngưỡng này sẽ được chia bằng
     * fixed-size fallback.
     */
    private static final int FALLBACK_THRESHOLD = 800;

    private static final int DEFAULT_FALLBACK_CHUNK_SIZE = 512;

    private static final int DEFAULT_FALLBACK_CHUNK_OVERLAP = 50;

    /**
     * Phát hiện ranh giới câu.
     *
     * Giữ lại dấu kết thúc câu nhưng bỏ khoảng trắng theo sau.
     */
    private static final Pattern SENTENCE_BOUNDARY =
            Pattern.compile(
                    "([.!?。！？]+)([\\s]+|$)"
            );

    @Override
    public List<ChunkDraft> chunk(
            String rawText,
            ChunkingStrategy strategy,
            ChunkingOptions options
    ) {
        List<ChunkDraft> result =
                new ArrayList<>();

        if (rawText == null
                || rawText.trim().isEmpty()) {

            return result;
        }

        List<String> sentences =
                splitIntoSentences(rawText);

        int chunkIndex = 0;

        for (String sentence : sentences) {
            if (sentence == null
                    || sentence.isBlank()) {

                continue;
            }

            String normalizedSentence =
                    sentence.trim();

            if (normalizedSentence.length()
                    > FALLBACK_THRESHOLD) {

                List<ChunkDraft> subChunks =
                        fixedSizeFallback(
                                normalizedSentence,
                                chunkIndex,
                                options
                        );

                result.addAll(subChunks);

                chunkIndex +=
                        subChunks.size();

            } else {
                result.add(
                        new ChunkDraft(
                                chunkIndex,
                                normalizedSentence,

                                /*
                                 * Worker sẽ gán trang thật.
                                 */
                                null,

                                normalizedSentence.length()
                        )
                );

                chunkIndex++;
            }
        }

        return result;
    }

    private List<String> splitIntoSentences(
            String text
    ) {
        List<String> sentences =
                new ArrayList<>();

        Matcher matcher =
                SENTENCE_BOUNDARY.matcher(text);

        int lastEnd = 0;

        while (matcher.find()) {
            String sentence =
                    text.substring(
                            lastEnd,
                            matcher.end(1)
                    ).strip();

            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String tail =
                    text.substring(lastEnd)
                            .strip();

            if (!tail.isEmpty()) {
                sentences.add(tail);
            }
        }

        return sentences;
    }

    private List<ChunkDraft> fixedSizeFallback(
            String text,
            int startIndex,
            ChunkingOptions options
    ) {
        List<ChunkDraft> chunks =
                new ArrayList<>();

        int chunkSize =
                options != null
                        && options.chunkSize() > 0
                        ? options.chunkSize()
                        : DEFAULT_FALLBACK_CHUNK_SIZE;

        int chunkOverlap =
                options != null
                        && options.chunkOverlap() >= 0
                        ? options.chunkOverlap()
                        : DEFAULT_FALLBACK_CHUNK_OVERLAP;

        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "chunkOverlap phải nhỏ hơn chunkSize."
            );
        }

        int start = 0;
        int textLength = text.length();
        int currentIndex = startIndex;

        while (start < textLength) {
            int end =
                    Math.min(
                            start + chunkSize,
                            textLength
                    );

            String content =
                    text.substring(
                            start,
                            end
                    );

            chunks.add(
                    new ChunkDraft(
                            currentIndex,
                            content,

                            /*
                             * Worker sẽ gán trang thật.
                             */
                            null,

                            content.length()
                    )
            );

            currentIndex++;

            if (end == textLength) {
                break;
            }

            int nextStart =
                    end - chunkOverlap;

            if (nextStart <= start) {
                nextStart = end;
            }

            start = nextStart;
        }

        return chunks;
    }
}