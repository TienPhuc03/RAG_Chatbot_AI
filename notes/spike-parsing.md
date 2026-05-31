# Spike: Tika vs PDFBox

| File           | Tika               | PDFBox             |
| -------------- | ------------------ | ------------------ |
| PDF tiếng Việt | ✅ Unicode đúng    | ✅ Unicode đúng    |
| PDF scan ảnh   | trả "" không crash | trả "" không crash |
| PPTX slide     | ✅ extract được    | ❌ không hỗ trợ    |

## Quyết định: dùng Tika

Lý do: 1 API xử lý được PDF + DOCX + PPTX luôn.
PDF scan: detect bằng cách check độ dài text < 50 ký tự.
