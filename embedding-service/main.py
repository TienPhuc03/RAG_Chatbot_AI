from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
from sentence_transformers import SentenceTransformer
import uvicorn

# Khởi tạo FastAPI
app = FastAPI(
    title="Embedding Service - Week 2 (REAL)",
    description="API chạy mô hình multilingual-e5-base thật sự",
    version="2.0.0"
)

# Quy tắc nhóm: Comment giải thích rõ ràng chức năng
# Tải mô hình AI thật từ HuggingFace về bộ nhớ máy (chỉ load 1 lần duy nhất khi khởi động)
print("Đang tải mô hình multilingual-e5-base... Vui lòng đợi...")
model = SentenceTransformer('intfloat/multilingual-e5-base')
print("Tải mô hình thành công!")

# Cấu trúc dữ liệu đầu vào đúng chuẩn List[str] của Jira
class EmbedRequest(BaseModel):
    texts: List[str]

# 1. API Endpoint xử lý đơn lẻ hoặc hàng loạt (gộp chung /embed theo thiết kế FastAPI thông thường)
@app.post("/embed")
async def get_embeddings(request: EmbedRequest):
    """
    API tiếp nhận danh sách văn bản, chạy qua mô hình AI thật 
    và trả về danh sách các vector số thực sự (List[List[float]])
    """
    try:
        if not request.texts:
            raise HTTPException(status_code=400, detail="Danh sách 'texts' không được trống")
        
        # Chạy mô hình thật để chuyển chữ thành các dãy số (Embedding Vectors)
        embeddings = model.encode(request.texts)
        
        # Chuyển đổi định dạng numpy array của mô hình về List truyền thống của Python để trả về JSON
        return embeddings.tolist()
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý mô hình AI: {str(e)}")

@app.get("/health")
async def health_check():
    return {"status": "healthy", "week": 2, "model": "multilingual-e5-base"}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
