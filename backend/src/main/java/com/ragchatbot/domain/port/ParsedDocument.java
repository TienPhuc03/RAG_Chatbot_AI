package com.ragchatbot.domain.port;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kết quả sau khi parse một tài liệu.
 *
 * @param title          tiêu đề tài liệu
 * @param rawText        toàn bộ text của tài liệu
 * @param sourceFileName tên file gốc
 * @param contentType    MIME type
 * @param metadata       metadata bổ sung
 * @param pages          danh sách trang vật lý đã parse
 */
public record ParsedDocument(
        String title,
        String rawText,
        String sourceFileName,
        String contentType,
        Map<String, String> metadata,
        List<ParsedPage> pages
) {

    public ParsedDocument {
        rawText = rawText == null
                ? ""
                : rawText;

        metadata = metadata == null
                ? Map.of()
                : Collections.unmodifiableMap(
                        new HashMap<>(metadata)
                );

        pages = pages == null
                ? List.of()
                : Collections.unmodifiableList(
                        new ArrayList<>(pages)
                );
    }

    /**
     * Constructor tương thích với code cũ.
     *
     * Những nơi còn tạo ParsedDocument với năm tham số
     * vẫn có thể compile.
     *
     * Khi không có thông tin từng trang, toàn bộ rawText
     * được xem như một trang duy nhất.
     */
    public ParsedDocument(
            String title,
            String rawText,
            String sourceFileName,
            String contentType,
            Map<String, String> metadata
    ) {
        this(
                title,
                rawText,
                sourceFileName,
                contentType,
                metadata,
                rawText == null || rawText.isBlank()
                        ? List.of()
                        : List.of(
                                new ParsedPage(
                                        1,
                                        rawText
                                )
                        )
        );
    }
}