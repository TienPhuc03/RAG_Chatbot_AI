package com.ragchatbot.infrastructure.benchmark;

/**
 * Trạng thái của một benchmark job đang chạy async.
 */
public enum BenchmarkJobStatus {
    /** Job đã được tạo, chưa bắt đầu. */
    PENDING,

    /** Job đang chạy. */
    RUNNING,

    /** Job đã hoàn thành thành công. */
    COMPLETED,

    /** Job đã thất bại do lỗi. */
    FAILED
}
