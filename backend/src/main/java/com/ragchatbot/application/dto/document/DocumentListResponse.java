package com.ragchatbot.application.dto.document;

import java.time.Instant;
import java.util.UUID;

import com.ragchatbot.domain.enums.DocumentStatus;

//DTO trả về khi FE muốn lấy danh sách tài liệu đã upload, bao gồm thông tin cơ bản và trạng thái hiện tại của từng tài liệu
public record DocumentListResponse(
        UUID id,
        String title,
        String sourceFileName,
        String courseCode,
        String courseName,      
        DocumentStatus status,
        Instant createdAt       //thời điểm upload tài liệu lên hệ thống
) {}