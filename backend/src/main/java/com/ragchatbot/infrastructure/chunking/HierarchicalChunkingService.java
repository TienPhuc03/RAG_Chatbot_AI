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
public class HierarchicalChunkingService
        implements ChunkingService {

    /**
     * Hai hoặc nhiều ký tự xuống dòng được xem như
     * ranh giới section.
     */
    private static final Pattern SECTION_BOUNDARY =
            Pattern.compile("\\n{2,}");

    /**
     * Nhận diện bullet hoặc danh sách đánh số.
     */
    private static final Pattern BULLET_LINE =
            Pattern.compile(
                    "^\\s*[-*•]\\s+"
                            + "|^\\s*\\d+\\.\\s+"
            );

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

        String[] sections =
                SECTION_BOUNDARY.split(
                        rawText.trim()
                );

        int chunkIndex = 0;

        for (String section : sections) {
            String trimmedSection =
                    section == null
                            ? ""
                            : section.strip();

            if (trimmedSection.isEmpty()) {
                continue;
            }

            /*
             * Chunk cha.
             */
            int parentIndex = chunkIndex;

            result.add(
                    new ChunkDraft(
                            chunkIndex,
                            trimmedSection,

                            /*
                             * Worker sẽ gán trang thật.
                             */
                            null,

                            trimmedSection.length(),

                            /*
                             * Chunk cha không có parent.
                             */
                            null
                    )
            );

            chunkIndex++;

            List<String> children =
                    splitIntoChildren(
                            trimmedSection
                    );

            /*
             * Chỉ tạo child nếu section thực sự
             * có thể chia thành nhiều phần.
             */
            if (children.size() > 1) {
                for (String child : children) {
                    String trimmedChild =
                            child == null
                                    ? ""
                                    : child.strip();

                    if (trimmedChild.isEmpty()) {
                        continue;
                    }

                    result.add(
                            new ChunkDraft(
                                    chunkIndex,
                                    trimmedChild,

                                    /*
                                     * Worker sẽ gán trang thật.
                                     */
                                    null,

                                    trimmedChild.length(),

                                    /*
                                     * Trỏ tới local index
                                     * của chunk cha.
                                     */
                                    parentIndex
                            )
                    );

                    chunkIndex++;
                }
            }
        }

        return result;
    }

    private List<String> splitIntoChildren(
            String sectionText
    ) {
        String[] lines =
                sectionText.split("\\n");

        boolean hasBullet = false;

        for (String line : lines) {
            if (BULLET_LINE
                    .matcher(line)
                    .find()) {

                hasBullet = true;
                break;
            }
        }

        if (hasBullet) {
            return splitByBullet(lines);
        }

        return splitBySentence(
                sectionText
        );
    }

    private List<String> splitByBullet(
            String[] lines
    ) {
        List<String> children =
                new ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        for (String line : lines) {
            if (BULLET_LINE
                    .matcher(line)
                    .find()) {

                if (!current.isEmpty()) {
                    children.add(
                            current.toString()
                                    .strip()
                    );
                }

                current =
                        new StringBuilder(
                                line.strip()
                        );

            } else {
                String normalizedLine =
                        line.strip();

                if (normalizedLine.isEmpty()) {
                    continue;
                }

                if (!current.isEmpty()) {
                    current.append(" ");
                }

                current.append(
                        normalizedLine
                );
            }
        }

        if (!current.isEmpty()) {
            children.add(
                    current.toString()
                            .strip()
            );
        }

        return children;
    }

    private List<String> splitBySentence(
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
}