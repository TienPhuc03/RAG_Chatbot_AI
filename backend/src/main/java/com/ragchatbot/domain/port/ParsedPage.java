package com.ragchatbot.domain.port;

/**
 * Đại diện cho một trang vật lý của tài liệu đã parse.
 *
 * @param pageNumber số trang vật lý, bắt đầu từ 1
 * @param text       nội dung text của trang
 */
public record ParsedPage(
        int pageNumber,
        String text
) {

    public ParsedPage {
        if (pageNumber < 1) {
            throw new IllegalArgumentException(
                    "pageNumber phải lớn hơn hoặc bằng 1."
            );
        }

        text = text == null ? "" : text;
    }
}