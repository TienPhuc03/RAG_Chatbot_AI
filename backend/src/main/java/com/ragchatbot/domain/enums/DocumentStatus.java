package com.ragchatbot.domain.enums;

/*
 *Trạng thái xử lý của tài liệu.
 *PROCESSING -> đang xử lý, INDEXED -> xong, FAILED -> lỗi.
 *FE polling mỗi 2s dùng enum này để hiển thị badge màu.
 */
public enum DocumentStatus {
    PROCESSING,
    INDEXED,
    FAILED
}