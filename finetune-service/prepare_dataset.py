"""
prepare_dataset.py
──────────────────
Pipeline tự động sinh dữ liệu huấn luyện fine-tuning từ slide môn học.

Luồng xử lý:
  1. Đọc tất cả file PPTX/PDF trong thư mục slides/
  2. Trích xuất text từng slide (dùng python-pptx hoặc pypdf2)
  3. Gọi Gemini API sinh cặp câu hỏi–đáp án từ nội dung slide
  4. Lưu ra train_dataset.json đúng format Hugging Face

Output: [{"instruction": "...", "output": "..."}, ...]
"""

import json
import os
import re
import time
from pathlib import Path

import google.generativeai as genai
from tqdm import tqdm

# Cấu hình 
GEMINI_API_KEY   = "YOUR_API_KEY_HERE"  
SLIDES_DIR       = RAW_DIR = Path(__file__).parent / "slides"               # Thư mục chứa file slide
OUTPUT_FILE      = Path(__file__).parent.parent / "train_dataset.json"      # File kết quả
QA_PER_SLIDE     = 3                                                        # Số cặp Q&A sinh ra mỗi slide
MIN_TEXT_LENGTH  = 80                                                       # Bỏ qua slide có text quá ngắn
DELAY_BETWEEN_CALLS = 7                                                   # Giây chờ giữa các lần gọi API
TEST_SET_PATH = Path(__file__).parent.parent / "datasets" / "benchmark" / "test_set.json"  


# Khởi tạo Gemini 
genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel("gemini-2.5-flash-lite")


# BƯỚC 1: Trích xuất text từ slide 

def extract_text_from_pptx(file_path: Path) -> list[str]:
    """
    Đọc file PPTX, trả về list text của từng slide.
    Bỏ qua slide trống hoặc chỉ có tiêu đề.
    """
    from pptx import Presentation

    prs = Presentation(file_path)
    slide_texts = []

    for slide_num, slide in enumerate(prs.slides, start=1):
        texts = []
        for shape in slide.shapes:
            # Chỉ lấy text từ shape có text frame
            if not shape.has_text_frame:
                continue
            for para in shape.text_frame.paragraphs:
                line = " ".join(run.text for run in para.runs).strip()
                if line:
                    texts.append(line)

        slide_text = "\n".join(texts)

        # Bỏ qua slide quá ngắn (thường là slide ảnh hoặc trống)
        if len(slide_text) >= MIN_TEXT_LENGTH:
            slide_texts.append(f"[Slide {slide_num}]\n{slide_text}")

    return slide_texts


def extract_text_from_pdf(file_path: Path) -> list[str]:
    """
    Đọc file PDF, trả về list text theo từng trang.
    """
    import PyPDF2

    page_texts = []
    with open(file_path, "rb") as f:
        reader = PyPDF2.PdfReader(f)
        for page_num, page in enumerate(reader.pages, start=1):
            text = page.extract_text() or ""
            text = text.strip()
            if len(text) >= MIN_TEXT_LENGTH:
                page_texts.append(f"[Trang {page_num}]\n{text}")

    return page_texts


def extract_all_slides(slides_dir: Path) -> list[dict]:
    """
    Quét toàn bộ file trong slides/, trích xuất text.
    Trả về list {"file": tên file, "content": text slide}
    """
    all_slides = []

    pptx_files = list(slides_dir.glob("*.pptx"))
    pdf_files  = list(slides_dir.glob("*.pdf"))
    all_files  = pptx_files + pdf_files

    if not all_files:
        print(f"⚠️  Không tìm thấy file nào trong {slides_dir}/")
        print("    Hãy đặt file .pptx hoặc .pdf vào thư mục slides/")
        return []

    print(f"📂 Tìm thấy {len(all_files)} file: "
          f"{len(pptx_files)} PPTX, {len(pdf_files)} PDF")

    for file_path in all_files:
        print(f"   📄 Đang đọc: {file_path.name}")
        try:
            if file_path.suffix.lower() == ".pptx":
                slides = extract_text_from_pptx(file_path)
            else:
                slides = extract_text_from_pdf(file_path)

            for slide_text in slides:
                all_slides.append({
                    "file": file_path.name,
                    "content": slide_text
                })

            print(f"      ✅ Trích xuất được {len(slides)} slide/trang có nội dung")

        except Exception as e:
            print(f"      ❌ Lỗi khi đọc {file_path.name}: {e}")

    return all_slides


# BƯỚC 2: Gọi Gemini sinh câu hỏi–đáp án 

def generate_qa_pairs(slide_content: str, num_pairs: int = QA_PER_SLIDE) -> list[dict]:
    """
    Gọi Gemini API sinh num_pairs cặp Q&A từ nội dung 1 slide.
    Tự động retry tối đa 3 lần khi gặp lỗi 429 rate limit.
    """
    prompt = f"""Bạn là chuyên gia giáo dục đại học về lĩnh vực lập trình và công nghệ thông tin.

Dưới đây là nội dung từ một slide bài giảng:
---
{slide_content}
---

Hãy sinh ra chính xác {num_pairs} cặp câu hỏi và đáp án dựa HOÀN TOÀN trên nội dung slide trên.

Yêu cầu quan trọng:
- Câu hỏi phải rõ ràng, cụ thể, đúng trọng tâm kiến thức
- Đáp án phải đầy đủ, chính xác, dựa trên slide (không bịa thêm)
- Viết bằng tiếng Việt, có dấu đầy đủ, không bị lỗi font
- Câu hỏi đa dạng: định nghĩa, giải thích, so sánh, ứng dụng

Trả lời CHỈ bằng JSON thuần túy (không có markdown, không có ```), theo đúng format:
[
  {{"instruction": "Câu hỏi 1?", "output": "Đáp án đầy đủ 1."}},
  {{"instruction": "Câu hỏi 2?", "output": "Đáp án đầy đủ 2."}}
]"""

    # Thử lại tối đa 3 lần nếu gặp rate limit
    max_retries = 3
    for attempt in range(max_retries):
        try:
            response = model.generate_content(prompt)
            raw_text = response.text.strip()

            # Làm sạch markdown code block nếu Gemini trả về
            raw_text = re.sub(r"```json\s*", "", raw_text)
            raw_text = re.sub(r"```\s*", "", raw_text)
            raw_text = raw_text.strip()

            # Parse JSON
            pairs = json.loads(raw_text)

            # Validate từng item
            validated = []
            for item in pairs:
                if isinstance(item, dict) and "instruction" in item and "output" in item:
                    if item["instruction"].strip() and item["output"].strip():
                        validated.append({
                            "instruction": item["instruction"].strip(),
                            "output": item["output"].strip()
                        })

            return validated

        except json.JSONDecodeError as e:
            # Lỗi parse JSON — không cần retry
            print(f"      ⚠️  JSON parse error: {e}")
            return []

        except Exception as e:
            error_msg = str(e)

            # Nếu là rate limit 429 và còn lượt retry → chờ rồi thử lại
            if "429" in error_msg and attempt < max_retries - 1:
                wait_sec = 65  # chờ 65 giây để quota per-minute reset
                print(f"\n      ⏳ Rate limit (lần {attempt + 1}/{max_retries}), "
                      f"chờ {wait_sec}s rồi thử lại...")
                time.sleep(wait_sec)
                continue  # thử lại vòng lặp

            # Lỗi khác hoặc đã hết retry
            print(f"      ⚠️  Gemini API error: {e}")
            return []

    return []


# BƯỚC 3: Pipeline chính 

def run_pipeline():
    print("=" * 60)
    print("  PIPELINE SINH DỮ LIỆU HUẤN LUYỆN FINE-TUNING")
    print("=" * 60)

    # Tạo thư mục slides nếu chưa có
    SLIDES_DIR.mkdir(exist_ok=True)

    # Trích xuất text từ tất cả slide
    print("\n📖 BƯỚC 1: Đọc và trích xuất text từ slide...")
    all_slides = extract_all_slides(SLIDES_DIR)

    if not all_slides:
        print("❌ Không có slide nào để xử lý. Dừng pipeline.")
        return

    print(f"\n✅ Tổng cộng {len(all_slides)} slide/trang cần xử lý")
    print(f"   Dự kiến sinh: ~{len(all_slides) * QA_PER_SLIDE} cặp Q&A\n")

    # Sinh Q&A cho từng slide
    print("🤖 BƯỚC 2: Gọi Gemini API sinh câu hỏi–đáp án...")
    dataset = []

    for slide in tqdm(all_slides, desc="Đang xử lý slide"):
        pairs = generate_qa_pairs(slide["content"], QA_PER_SLIDE)
        dataset.extend(pairs)

        # Tránh rate limit của Gemini API
        time.sleep(DELAY_BETWEEN_CALLS)

    # Shuffle để trộn đều dữ liệu từ các file
    import random
    random.shuffle(dataset)

    # Lưu ra file JSON
    print(f"\n💾 BƯỚC 3: Lưu {len(dataset)} cặp Q&A ra {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(dataset, f, ensure_ascii=False, indent=2)

    print("\n" + "=" * 60)
    print(f"  ✅ HOÀN THÀNH!")
    print(f"  📊 Tổng cặp Q&A: {len(dataset)}")
    print(f"  📁 File output : {OUTPUT_FILE.absolute()}")
    print("=" * 60)

    # Verify kết quả
    verify_output(OUTPUT_FILE)


def verify_output(output_file: Path):
    """
    Kiểm tra file output có đúng chuẩn không.
    """
    print("\n🔍 VERIFY kết quả...")
    try:
        with open(output_file, "r", encoding="utf-8") as f:
            data = json.load(f)

        errors = []
        for i, item in enumerate(data):
            if not isinstance(item, dict):
                errors.append(f"  Item {i}: không phải dict")
                continue
            if "instruction" not in item:
                errors.append(f"  Item {i}: thiếu 'instruction'")
            if "output" not in item:
                errors.append(f"  Item {i}: thiếu 'output'")
            if not item.get("instruction", "").strip():
                errors.append(f"  Item {i}: 'instruction' rỗng")
            if not item.get("output", "").strip():
                errors.append(f"  Item {i}: 'output' rỗng")

        if errors:
            print(f"  ❌ Tìm thấy {len(errors)} lỗi:")
            for e in errors[:5]:
                print(e)
        else:
            print(f"  ✅ JSON hợp lệ — {len(data)} cặp Q&A")
            print(f"  ✅ Tiếng Việt hiển thị đúng")
            print(f"  ✅ Đúng format Hugging Face")

            # Hiện 2 ví dụ đầu
            print("\n📝 Ví dụ 2 cặp Q&A đầu tiên:")
            for item in data[:2]:
                print(f"\n  Q: {item['instruction']}")
                print(f"  A: {item['output'][:100]}...")

    except Exception as e:
        print(f"  ❌ Lỗi verify: {e}")


#  Chạy script 
if __name__ == "__main__":
    run_pipeline()