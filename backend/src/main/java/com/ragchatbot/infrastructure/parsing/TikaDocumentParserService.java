package com.ragchatbot.infrastructure.parsing;

import com.ragchatbot.domain.port.DocumentParserService;
import com.ragchatbot.domain.port.ParsedDocument;
import com.ragchatbot.domain.port.ParsedPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TikaDocumentParserService
        implements DocumentParserService {

    /**
     * Ngưỡng tối thiểu để xác định tài liệu có text.
     */
    private static final int MIN_TEXT_LENGTH = 50;

    @Override
    public ParsedDocument parse(
            byte[] content,
            String fileName,
            String contentType
    ) {
        if (content == null || content.length == 0) {
            throw new DocumentParseException(
                    "File không có dữ liệu."
            );
        }

        /*
         * PDF cần parse riêng từng trang bằng PDFBox.
         */
        if (isPdf(fileName, contentType)) {
            return parsePdfByPage(
                    content,
                    fileName,
                    contentType
            );
        }

        /*
         * DOCX, PPTX và các định dạng khác tiếp tục dùng Tika.
         */
        return parseNonPdfWithTika(
                content,
                fileName,
                contentType
        );
    }

    /**
     * Parse PDF theo từng trang vật lý.
     *
     * Trang được đánh số từ 1 đến tổng số trang PDF.
     * Trang rỗng vẫn được giữ lại để không làm lệch số trang.
     */
    private ParsedDocument parsePdfByPage(
            byte[] content,
            String fileName,
            String contentType
    ) {
        try (PDDocument pdfDocument =
                     PDDocument.load(content)) {

            PDFTextStripper textStripper =
                    new PDFTextStripper();

            /*
             * Cố gắng giữ thứ tự text theo vị trí hiển thị.
             */
            textStripper.setSortByPosition(true);

            List<ParsedPage> parsedPages =
                    new ArrayList<>();

            StringBuilder fullText =
                    new StringBuilder();

            int totalPages =
                    pdfDocument.getNumberOfPages();

            for (int pageNumber = 1;
                 pageNumber <= totalPages;
                 pageNumber++) {

                textStripper.setStartPage(pageNumber);
                textStripper.setEndPage(pageNumber);

                String pageText =
                        textStripper.getText(
                                pdfDocument
                        );

                pageText = pageText == null
                        ? ""
                        : pageText.strip();

                /*
                 * Luôn giữ trang, kể cả trang không có text.
                 * Điều này giúp pageNumber không bị lệch.
                 */
                parsedPages.add(
                        new ParsedPage(
                                pageNumber,
                                pageText
                        )
                );

                if (!pageText.isBlank()) {
                    if (!fullText.isEmpty()) {
                        fullText.append("\n\n");
                    }

                    fullText.append(pageText);
                }
            }

            String extractedText =
                    fullText.toString().strip();

            validatePdfText(
                    extractedText,
                    fileName
            );

            String title = null;

            if (pdfDocument
                    .getDocumentInformation()
                    != null) {

                title = pdfDocument
                        .getDocumentInformation()
                        .getTitle();
            }

            if (title == null || title.isBlank()) {
                title = stripExtension(fileName);
            }

            Map<String, String> metadata =
                    new HashMap<>();

            metadata.put(
                    "contentType",
                    contentType == null
                            ? "application/pdf"
                            : contentType
            );

            metadata.put(
                    "pageCount",
                    String.valueOf(totalPages)
            );

            return new ParsedDocument(
                    title,
                    extractedText,
                    fileName,
                    contentType,
                    metadata,
                    parsedPages
            );

        } catch (InvalidPasswordException exception) {
            throw new DocumentParseException(
                    "PDF được bảo vệ bằng mật khẩu: "
                            + fileName,
                    exception
            );

        } catch (IOException exception) {
            throw new DocumentParseException(
                    "Không thể đọc PDF hoặc PDF dạng ảnh/hỏng: "
                            + fileName
                            + " — "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    /**
     * Parse DOCX, PPTX hoặc định dạng khác bằng Apache Tika.
     *
     * Do corpus benchmark hiện dùng PDF, các định dạng khác
     * được giữ tương thích bằng cách xem toàn bộ tài liệu
     * như một trang duy nhất.
     */
    private ParsedDocument parseNonPdfWithTika(
            byte[] content,
            String fileName,
            String contentType
    ) {
        Parser parser =
                new AutoDetectParser();

        BodyContentHandler handler =
                new BodyContentHandler(-1);

        Metadata tikaMetadata =
                new Metadata();

        ParseContext parseContext =
                new ParseContext();

        tikaMetadata.set(
                TikaCoreProperties.RESOURCE_NAME_KEY,
                fileName
        );

        try (InputStream inputStream =
                     new ByteArrayInputStream(content)) {

            parser.parse(
                    inputStream,
                    handler,
                    tikaMetadata,
                    parseContext
            );

        } catch (TikaException
                 | IOException
                 | SAXException exception) {

            throw new DocumentParseException(
                    "Không thể parse file: "
                            + fileName
                            + " — "
                            + exception.getMessage(),
                    exception
            );
        }

        String rawText =
                handler.toString();

        rawText = rawText == null
                ? ""
                : rawText.strip();

        validateGenericText(
                rawText,
                fileName
        );

        String title =
                tikaMetadata.get(
                        TikaCoreProperties.TITLE
                );

        if (title == null || title.isBlank()) {
            title = stripExtension(fileName);
        }

        Map<String, String> metadata =
                new HashMap<>();

        String detectedContentType =
                tikaMetadata.get(
                        Metadata.CONTENT_TYPE
                );

        if (detectedContentType != null) {
            metadata.put(
                    "contentType",
                    detectedContentType
            );
        } else if (contentType != null) {
            metadata.put(
                    "contentType",
                    contentType
            );
        }

        metadata.put("pageCount", "1");

        return new ParsedDocument(
                title,
                rawText,
                fileName,
                contentType,
                metadata,
                List.of(
                        new ParsedPage(
                                1,
                                rawText
                        )
                )
        );
    }

    private void validatePdfText(
            String rawText,
            String fileName
    ) {
        if (rawText == null
                || rawText.strip().length()
                < MIN_TEXT_LENGTH) {

            throw new DocumentParseException(
                    "File "
                            + fileName
                            + " là PDF dạng ảnh hoặc không có đủ text. "
                            + "Vui lòng dùng PDF có text."
            );
        }
    }

    private void validateGenericText(
            String rawText,
            String fileName
    ) {
        if (rawText == null
                || rawText.strip().length()
                < MIN_TEXT_LENGTH) {

            throw new DocumentParseException(
                    "File "
                            + fileName
                            + " không có đủ nội dung text để xử lý."
            );
        }
    }

    private boolean isPdf(
            String fileName,
            String contentType
    ) {
        boolean pdfContentType =
                contentType != null
                        && contentType
                        .toLowerCase(Locale.ROOT)
                        .contains("pdf");

        boolean pdfExtension =
                fileName != null
                        && fileName
                        .toLowerCase(Locale.ROOT)
                        .endsWith(".pdf");

        return pdfContentType || pdfExtension;
    }

    private String stripExtension(
            String fileName
    ) {
        if (fileName == null || fileName.isBlank()) {
            return "Untitled";
        }

        int dotIndex =
                fileName.lastIndexOf('.');

        return dotIndex > 0
                ? fileName.substring(0, dotIndex)
                : fileName;
    }
}