package com.ragchatbot.infrastructure.parsing;

/**
 * Exception ném ra khi không thể parse tài liệu.
 * Hai trường hợp chính:
 *   1. File bị lỗi hoặc định dạng không hỗ trợ
 *   2. PDF dạng scan (chỉ có ảnh, không có text)
 */
public class DocumentParseException extends RuntimeException {

    public DocumentParseException(String message) {
        super(message);
    }

    public DocumentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}