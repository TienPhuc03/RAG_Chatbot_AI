package com.ragchatbot.infrastructure.chunking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ragchatbot.domain.enums.ChunkingStrategy;
import com.ragchatbot.domain.port.ChunkDraft;
import com.ragchatbot.domain.port.ChunkingOptions;
import com.ragchatbot.domain.port.ChunkingService;

@Service
public class HierarchicalChunkingService  implements ChunkingService {

    private static final Pattern SECTION_BOUNDARY = Pattern.compile("\\n{2,}");

    private static final Pattern BULLET_LINE = Pattern.compile(
        "^\\s*[-*•]\\s+|^\\s*\\d+\\.\\s+"
    );

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
        "([.!?。！？]+)([\\s]+|$)"
    );

    @Override
    public List<ChunkDraft> chunk(String rawText, ChunkingStrategy strategy, ChunkingOptions options) {
        List<ChunkDraft> result = new ArrayList<>();

        if (rawText == null || rawText.trim().isEmpty()) {
            return result;
        }

        // Tách thành các section (chunk cha - level 1)
        String[] sections = SECTION_BOUNDARY.split(rawText.trim());

        int chunkIndex = 0;

        for (String section : sections) {
            String trimmedSection = section.strip();
            if (trimmedSection.isEmpty()) continue;

            // Tạo chunk cha (parentChunkId = null)
            int parentIndex = chunkIndex;
            result.add(new ChunkDraft(
                chunkIndex++,
                trimmedSection,
                1,
                trimmedSection.length(),
                null  // chunk cha không có parent
            ));

            // Tách chunk con (level 2) từ nội dung section
            List<String> children = splitIntoChildren(trimmedSection);

            // Chỉ tạo chunk con nếu section thực sự chia được thành nhiều phần
            if (children.size() > 1) {
                for (String child : children) {
                    String trimmedChild = child.strip();
                    if (trimmedChild.isEmpty()) continue;

                    result.add(new ChunkDraft(
                        chunkIndex++,
                        trimmedChild,
                        1,
                        trimmedChild.length(),
                        parentIndex  // trỏ về chunkIndex của chunk cha
                    ));
                }
            }
        }

        return result;
    }

    private List<String> splitIntoChildren(String sectionText) {
        String[] lines = sectionText.split("\\n");

        // Kiểm tra có phải dạng bullet list không
        boolean hasBullet = false;
        for (String line : lines) {
            if (BULLET_LINE.matcher(line).find()) {
                hasBullet = true;
                break;
            }
        }

        if (hasBullet) {
            return splitByBullet(lines);
        } else {
            return splitBySentence(sectionText);
        }
    }

    private List<String> splitByBullet(String[] lines) {
        List<String> children = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (BULLET_LINE.matcher(line).find()) {
                // Bắt đầu bullet mới → lưu bullet trước nếu có
                if (!current.isEmpty()) {
                    children.add(current.toString().strip());
                }
                current = new StringBuilder(line.strip());
            } else {
                // Dòng tiếp theo không phải bullet → nối vào bullet hiện tại
                if (!current.isEmpty()) {
                    current.append(" ").append(line.strip());
                } else {
                    current.append(line.strip());
                }
            }
        }

        if (!current.isEmpty()) {
            children.add(current.toString().strip());
        }

        return children;
    }

    private List<String> splitBySentence(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_BOUNDARY.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.end(1)).strip();
            if (!sentence.isEmpty()) sentences.add(sentence);
            lastEnd = matcher.end();
        }

        // Phần đuôi không có dấu câu
        if (lastEnd < text.length()) {
            String tail = text.substring(lastEnd).strip();
            if (!tail.isEmpty()) sentences.add(tail);
        }

        return sentences;
    }
}


