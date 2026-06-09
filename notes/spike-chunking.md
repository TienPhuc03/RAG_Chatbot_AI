<div style="background-color: #17375e; color: white; padding: 25px; border-radius: 12px; font-family: sans-serif;">

<div style="text-align: center;">
<span style="font-size: 180%;">LANGCHAIN4J DOCUMENRTSPLITTER DOCS</span>
</div>
<div style="text-align: center;">
<span style="font-size: 180%;">PAPER CHUNKING ASTATEGIES</span>
</div>

</div>

> <span style="color: #074188; font-size: 20px; font-weight: bold;">1. Tổng quan về Chiến lược Chunking (Paper Research)</span>
</div>
Trong hệ thống Retrieval-Augmented Generation (RAG), Chunking (phân đoạn văn bản) đóng vai trò
quyết định cấu trúc ngữ nghĩa đầu vào cho Vector Database và LLM. Phân đoạn quá lớn gây nhiễu và
loãng thông tin, trong khi phân đoạn quá nhỏ làm mất đi ngữ cảnh mạch lạc của câu thoại.


- **Fixed-size Chunking (Kích thước cố định):** Chia đoạn dựa vào số lượng ký tự hoặc số lượng
Token cố định độc lập với cấu trúc văn bản. Luôn cần một khoảng chồng lấp (Overlap) để bảo toàn
thông tin chuyển tiếp tại ranh giới của đoạn.

- **Recursive Character-based Chunking (Phân đoạn đệ quy):** Chia nhỏ văn bản dựa trên một
danh sách các ký tự phân tách ưu tiên giảm dần (ví dụ: newline kép, newline đơn, khoảng trắng).
Chiến lược này cố gắng giữ trọn vẹn các cấu trúc ngữ nghĩa lớn như đoạn văn, câu trước khi bắt
buộc phải bẻ gãy do vượt quá giới hạn độ dài.

- **Document Structure-based Chunking (Phân đoạn theo cấu trúc):** Sử dụng cú pháp nguyên bản
của tài liệu (Markdown Headers, HTML tags, JSON keys, Table cells) để tạo phân đoạn. Phù hợp
nhất cho tài liệu kỹ thuật vì giữ nguyên ngữ cảnh phân cấp cấu trúc.

- **Semantic Chunking (Phân đoạn ngữ nghĩa):** Tính toán khoảng cách cosine của Embedding giữa
các câu liên tiếp. Điểm phân tách phân đoạn được xác định động tại vị trí mà sự thay đổi ngữ nghĩa
vượt quá một ngưỡng (Threshold) nhất định.

> <span style="color: #074188; font-size: 20px; font-weight: bold;">2. Cơ chế DocumentSplitter trong LangChain4j</span>
</div>

LangChain4j cung cấp interface DocumentSplitter nhằm trừu tượng hóa việc phân tách đối tượng
Document thành danh sách các TextSegment. Điểm đặc biệt của thư viện này là tích hợp sẵn cơ chế
kiểm tra token tự động thông qua Tokenizer để đảm bảo độ chính xác tuyệt đối khi tích hợp với
LLMs.

**Cấu trúc phân cấp các Splitter phổ biến trong LangChain4j:**

| Tên Class Splitter | Cơ chế hoạt động & Ứng dụng thực tế |
| :--- | :--- |
| `DocumentByParagraphSplitter` | Phân tách văn bản dựa trên ký tự xuống dòng kép (Double Newline). Ghép các đoạn ngắn lại miễn là không vượt quá giới hạn cấu hình. Phù hợp cho văn bản văn xuôi, bài báo khoa học. |
| `DocumentByLineSplitter` | Phân tách dựa theo từng dòng đơn. Thường áp dụng để xử lý log file hoặc dữ liệu dạng bảng thô theo hàng. |
| `DocumentBySentenceSplitter` | Sử dụng thư viện xử lý ngôn ngữ tự nhiên (hoặc Regex nâng cao) để nhận diện điểm kết thúc câu (dấu chấm, dấu hỏi chấm). Đảm bảo tính toàn vẹn của một tư duy truyền tải độc lập. |
| `DocumentByCharSplitter` | Cắt text thuần túy theo số lượng ký tự tối đa mà không màng tới ranh giới từ hay câu. Ít dùng trừ trường hợp dữ liệu dạng chuỗi mã hóa đặc biệt. |

**Cách thức khởi tạo và hoạt động nội bộ:**

Khi khởi tạo một DocumentSplitter, chúng ta cung cấp hai tham số chủ lực: maxSegmentSize và
maxOverlapSize. Đơn vị tính của hai tham số này phụ thuộc hoàn toàn vào việc bạn có truyền vào
một thực thể Tokenizer (như OpenAiTokenizer, HuggingFaceTokenizer) hay không:

- **Nếu dùng Tokenizer:** Kích thước được đo bằng đơn vị Token. Điều này chuẩn hóa độ dài chính
xác khi đưa vào Context Window của LLM.

- **Nếu KHÔNG dùng Tokenizer:** Kích thước mặc định tính theo **Ký tự (Characters)**.