package com.ragchatbot.infrastructure.parsing;

import com.ragchatbot.domain.port.DocumentParserService;
import com.ragchatbot.domain.port.ParsedDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse PDF, DOCX, PPTX thành plain text dùng Apache Tika.
 * Được gọi đầu tiên trong pipeline xử lý tài liệu,
 * trước bước chunking và embedding.
 */
@Service
public class TikaDocumentParserService implements DocumentParserService {

    // Ngưỡng tối thiểu số ký tự để phân biệt PDF có text và PDF scan (ảnh).
    // PDF scan: Tika parse thành công nhưng trả về text rỗng hoặc rất ngắn.
    private static final int MIN_TEXT_LENGTH = 50;

    @Override
    public ParsedDocument parse(byte[] content, String fileName, String contentType) {

        // AutoDetectParser tự nhận dạng định dạng file (PDF/DOCX/PPTX)
        // dựa trên magic bytes và metadata — không cần switch-case thủ công.
        Parser parser = new AutoDetectParser();

        // -1 bỏ giới hạn 100KB text output mặc định của Tika.
        // Slide bài giảng nhiều trang sẽ bị cắt nếu không set giá trị này.
        BodyContentHandler handler = new BodyContentHandler(-1);

        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        // Cung cấp tên file để Tika nhận dạng đúng định dạng,
        // kể cả khi content-type header bị sai hoặc thiếu.
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);

        try (InputStream stream = new ByteArrayInputStream(content)) {
            // Thực hiện parse — text ghi vào handler, metadata ghi vào metadata object.
            parser.parse(stream, handler, metadata, context);
        } catch (Exception e) {
            // Bọc lỗi kỹ thuật của Tika thành exception của domain để tầng trên xử lý.
            throw new DocumentParseException(
                "Không thể parse file: " + fileName + " — " + e.getMessage(), e
            );
        }

        String rawText = handler.toString();

        // Phát hiện PDF scan: Tika không báo lỗi nhưng không extract được text.
        // Thông báo rõ cho người dùng thay vì truyền text rỗng xuống chunker.
        if (rawText == null || rawText.trim().length() < MIN_TEXT_LENGTH) {
            throw new DocumentParseException(
                "File này là PDF dạng ảnh, không đọc được text. " +
                "Vui lòng dùng file PDF có text hoặc DOCX."
            );
        }

        // Ưu tiên lấy title từ metadata của file (tác giả điền khi tạo file).
        // Nếu không có thì dùng tên file bỏ phần đuôi mở rộng làm title.
        String title = metadata.get(TikaCoreProperties.TITLE);
        if (title == null || title.isBlank()) {
            title = stripExtension(fileName);
        }

        // Thu thập metadata hữu ích cho bước hiển thị và xử lý sau này.
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("contentType", metadata.get(Metadata.CONTENT_TYPE));

        // Lấy số trang nếu có — dùng để hiển thị trên UI danh sách tài liệu.
        String pageCount = metadata.get("xmpTPg:NPages");
        if (pageCount != null) {
            metadataMap.put("pageCount", pageCount);
        }

        return new ParsedDocument(title, rawText.trim(), fileName, contentType, metadataMap);
    }

    // Bỏ phần đuôi mở rộng để lấy tên file thuần làm title fallback.
    // Ví dụ: "chuong-1-java.pdf" → "chuong-1-java"
    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}