package com.ragchatbot.application.dto.document;

import java.util.UUID;

import com.ragchatbot.domain.enums.DocumentStatus;

//DTO trả về sau khi upload tài liệu, bao gồm thông tin cơ bản và trạng thái hiện tại của tài liệu đó
public record DocumentUploadResponse(
        UUID id,                //id
        String title,           //tiêu đề tài liệu
        String sourceFileName,  //tên file gốc khi upload lên
        String courseCode,      //mã môn học (nếu có)
        DocumentStatus status   //trạng thái hiện tại của tài liệu (PENDING, PROCESSING, INDEXED, FAILED)
) {}