package com.ragchatbot;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.ragchatbot.domain.port.ParsedDocument;
import com.ragchatbot.infrastructure.parsing.DocumentParseException;
import com.ragchatbot.infrastructure.parsing.TikaDocumentParserService;

class TikaDocumentParserServiceTest {

    // Khởi tạo trực tiếp, không cần Spring context vì class không có dependency nào.
    private final TikaDocumentParserService service = new TikaDocumentParserService();

    // Đọc file test từ src/test/resources/parsing/ thành mảng byte.
    private byte[] loadTestFile(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/parsing/" + name)) {
            // Dừng test ngay nếu file không tồn tại — lỗi setup chứ không phải lỗi code.
            assertThat(in).as("Không tìm thấy file test: " + name).isNotNull();
            return in.readAllBytes();
        }
    }

    @Test
    void parsesPdfWithVietnameseUnicode() throws Exception {
        byte[] content = loadTestFile("sample-vietnamese.pdf");

        ParsedDocument result = service.parse(
            content, "sample-vietnamese.pdf", "application/pdf"
        );

        // Kiểm tra text không bị rỗng sau khi parse.
        assertThat(result.rawText()).isNotBlank();

        // Kiểm tra ký tự tiếng Việt còn nguyên, không bị lỗi encoding.
        assertThat(result.rawText())
            .containsAnyOf("à", "á", "ă", "â", "ê", "ô", "ơ", "ư", "đ");

        // Kiểm tra tên file và title được gán đúng.
        assertThat(result.sourceFileName()).isEqualTo("sample-vietnamese.pdf");
        assertThat(result.title()).isNotBlank();
    }

    @Test
    void parsesDocxFile() throws Exception {
        byte[] content = loadTestFile("sample-slide.docx");

        ParsedDocument result = service.parse(
            content,
            "sample-slide.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );

        // Kiểm tra Tika extract được text từ DOCX.
        assertThat(result.rawText()).isNotBlank();
    }

    @Test
    void parsesPptxFile() throws Exception {
        byte[] content = loadTestFile("sample-slide.pptx");

        ParsedDocument result = service.parse(
            content,
            "sample-slide.pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        );

        // Kiểm tra Tika extract được text từ PPTX (PDFBox không làm được việc này).
        assertThat(result.rawText()).isNotBlank();
    }

    @Test
    void throwsExceptionForScannedPdf() {
        // Tạo byte array giả dạng PDF header nhưng không có nội dung text.
        // Mô phỏng PDF scan: Tika parse xong nhưng không extract được text.
        byte[] minimalPdf = "%PDF-1.4\n%%EOF".getBytes();

        // Kiểm tra service ném đúng exception thay vì trả về text rỗng.
        assertThatThrownBy(() ->
            service.parse(minimalPdf, "scan.pdf", "application/pdf")
        ).isInstanceOf(DocumentParseException.class);
    }
    /*
 * Kiểm tra message lỗi tiếng Việt khi parse PDF scan (toàn ảnh).
 * FE dùng message này để hiển thị hướng dẫn cho người dùng.
 */
@Test
void throwsVietnameseMessageForScannedPdf() {
    //PDF tối thiểu, không có text content — mô phỏng PDF scan
    byte[] minimalPdf = "%PDF-1.4\n%%EOF".getBytes();

    assertThatThrownBy(() ->
        service.parse(minimalPdf, "scan.pdf", "application/pdf")
    )
    .isInstanceOf(DocumentParseException.class)
    //message phải chứa "PDF dạng ảnh" để FE hiển thị hướng dẫn đúng
    .hasMessageContaining("PDF dạng ảnh");
}

/*
 * Kiểm tra PPTX có hình không crash JVM.
 * Tika xử lý được → rawText không null.
 * PPTX toàn hình → ném DocumentParseException, không crash.
 */
@Test
void parsesPptxWithImageSlideDoesNotCrash() throws Exception {
    byte[] content = loadTestFile("sample-slide.pptx");

    try {
        ParsedDocument result = service.parse(
            content,
            "sample-slide.pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        );
        //PPTX có text: parse thành công
        assertThat(result.rawText()).isNotNull();

    } catch (DocumentParseException e) {
        //PPTX toàn hình: ném đúng exception, không crash JVM
        assertThat(e).isInstanceOf(DocumentParseException.class);
    }
}
}