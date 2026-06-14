package com.ragchatbot.application.dto.document;

import com.ragchatbot.domain.enums.DocumentStatus;
import java.time.Instant;
import java.util.UUID;

/*
 *Response trả về cho FE khi polling GET /api/documents/{id}/status.
 *FE dùng status để đổi màu badge, dừng polling khi INDEXED hoặc FAILED.
 */
public record DocumentStatusResponse(
        UUID id,
        DocumentStatus status,
        Instant indexedAt
) {}