package com.ragchatbot;

import com.ragchatbot.domain.port.ParsedDocument;
import com.ragchatbot.infrastructure.parsing.DocumentParseException;
import com.ragchatbot.infrastructure.parsing.TikaDocumentParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TikaDocumentParserServiceTest {

    private final TikaDocumentParserService service =
            new TikaDocumentParserService();

    private byte[] loadTestFile(
            String fileName
    ) throws IOException {

        try (InputStream inputStream =
                     getClass()
                             .getResourceAsStream(
                                     "/parsing/"
                                             + fileName
                             )) {

            assertThat(inputStream)
                    .as(
                            "Không tìm thấy file test: "
                                    + fileName
                    )
                    .isNotNull();

            return inputStream.readAllBytes();
        }
    }

    @Test
    void parsesPdfWithVietnameseUnicode()
            throws Exception {

        byte[] content =
                loadTestFile(
                        "sample-vietnamese.pdf"
                );

        ParsedDocument result =
                service.parse(
                        content,
                        "sample-vietnamese.pdf",
                        "application/pdf"
                );

        assertThat(result.rawText())
                .isNotBlank();

        assertThat(result.rawText())
                .containsAnyOf(
                        "à",
                        "á",
                        "ă",
                        "â",
                        "ê",
                        "ô",
                        "ơ",
                        "ư",
                        "đ"
                );

        assertThat(result.sourceFileName())
                .isEqualTo(
                        "sample-vietnamese.pdf"
                );

        assertThat(result.title())
                .isNotBlank();

        assertThat(result.pages())
                .isNotEmpty();

        assertThat(
                result.pages().get(0).pageNumber()
        ).isEqualTo(1);
    }

    @Test
    void parsesDocxFile()
            throws Exception {

        byte[] content =
                loadTestFile(
                        "sample-slide.docx"
                );

        ParsedDocument result =
                service.parse(
                        content,
                        "sample-slide.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                );

        assertThat(result.rawText())
                .isNotBlank();

        assertThat(result.pages())
                .hasSize(1);

        assertThat(
                result.pages().get(0).pageNumber()
        ).isEqualTo(1);
    }

    @Test
    void parsesPptxFile()
            throws Exception {

        byte[] content =
                loadTestFile(
                        "sample-slide.pptx"
                );

        ParsedDocument result =
                service.parse(
                        content,
                        "sample-slide.pptx",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                );

        assertThat(result.rawText())
                .isNotBlank();
    }

    @Test
    void throwsExceptionForScannedPdf() {
        byte[] minimalPdf =
                "%PDF-1.4\n%%EOF"
                        .getBytes();

        assertThatThrownBy(
                () ->
                        service.parse(
                                minimalPdf,
                                "scan.pdf",
                                "application/pdf"
                        )
        ).isInstanceOf(
                DocumentParseException.class
        );
    }

    @Test
    void throwsVietnameseMessageForScannedPdf() {
        byte[] minimalPdf =
                "%PDF-1.4\n%%EOF"
                        .getBytes();

        assertThatThrownBy(
                () ->
                        service.parse(
                                minimalPdf,
                                "scan.pdf",
                                "application/pdf"
                        )
        )
                .isInstanceOf(
                        DocumentParseException.class
                )
                .hasMessageContaining(
                        "PDF dạng ảnh"
                );
    }

    @Test
    void parsesPptxWithImageSlideDoesNotCrash()
            throws Exception {

        byte[] content =
                loadTestFile(
                        "sample-slide.pptx"
                );

        try {
            ParsedDocument result =
                    service.parse(
                            content,
                            "sample-slide.pptx",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    );

            assertThat(result.rawText())
                    .isNotNull();

        } catch (DocumentParseException exception) {
            assertThat(exception)
                    .isInstanceOf(
                            DocumentParseException.class
                    );
        }
    }

    /**
     * Test quan trọng:
     * tạo PDF hai trang và kiểm tra parser không làm mất
     * ranh giới trang vật lý.
     */
    @Test
    void parsesPdfAndPreservesPhysicalPageNumbers()
            throws Exception {

        byte[] pdfBytes;

        try (PDDocument pdfDocument =
                     new PDDocument();

             ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            PDPage firstPage =
                    new PDPage();

            pdfDocument.addPage(
                    firstPage
            );

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(
                                 pdfDocument,
                                 firstPage
                         )) {

                contentStream.beginText();

                contentStream.setFont(
                        PDType1Font.HELVETICA,
                        12
                );

                contentStream.newLineAtOffset(
                        50,
                        700
                );

                contentStream.showText(
                        "First page contains enough searchable "
                                + "text for parser validation."
                );

                contentStream.endText();
            }

            PDPage secondPage =
                    new PDPage();

            pdfDocument.addPage(
                    secondPage
            );

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(
                                 pdfDocument,
                                 secondPage
                         )) {

                contentStream.beginText();

                contentStream.setFont(
                        PDType1Font.HELVETICA,
                        12
                );

                contentStream.newLineAtOffset(
                        50,
                        700
                );

                contentStream.showText(
                        "Second page contains the unique marker "
                                + "SECOND_PAGE_MARKER."
                );

                contentStream.endText();
            }

            pdfDocument.save(
                    outputStream
            );

            pdfBytes =
                    outputStream.toByteArray();
        }

        ParsedDocument result =
                service.parse(
                        pdfBytes,
                        "two-pages.pdf",
                        "application/pdf"
                );

        assertThat(result.pages())
                .hasSize(2);

        assertThat(
                result.pages()
                        .get(0)
                        .pageNumber()
        ).isEqualTo(1);

        assertThat(
                result.pages()
                        .get(1)
                        .pageNumber()
        ).isEqualTo(2);

        assertThat(
                result.pages()
                        .get(0)
                        .text()
        ).contains(
                "First page"
        );

        assertThat(
                result.pages()
                        .get(1)
                        .text()
        ).contains(
                "SECOND_PAGE_MARKER"
        );

        assertThat(result.rawText())
                .contains(
                        "First page",
                        "SECOND_PAGE_MARKER"
                );

        assertThat(
                result.metadata()
                        .get("pageCount")
        ).isEqualTo("2");
    }
}