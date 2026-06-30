package com.ragchatbot.application.dto.citation;

import java.util.UUID;

/*
 * Response trả về khi FE click vào citation chip trong câu trả lời.
 * Hiển thị trong modal: đoạn text gốc, tên file, số trang.
 *
 * @param chunkId        UUID của chunk được trích dẫn
 * @param content        Nội dung gốc của chunk (đoạn text)
 * @param sourceFileName Tên file tài liệu gốc
 * @param pageNumber     Số trang trong tài liệu (null nếu không xác định)
 * @param documentTitle  Tiêu đề tài liệu, hiển thị cho người dùng dễ nhận biết
 */
public record CitationDetailResponse(
        UUID chunkId,
        String content,
        String sourceFileName,
        Integer pageNumber,
        String documentTitle
) {}