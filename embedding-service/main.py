from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from sentence_transformers import SentenceTransformer
import uvicorn

from bge_m3_embedding_service import BgeM3EmbeddingService
from phobert_embedding_service import PhoBertEmbeddingService

app = FastAPI(
    title="Embedding Service - Week 4",
    description="API chạy đa mô hình nhúng (E5-Base, BGE-M3, & PhoBERT)",
    version="4.0.0"
)

print("Đang tải mô hình mặc định: multilingual-e5-base (Tuần 2)...")
default_model = SentenceTransformer('intfloat/multilingual-e5-base')

print("Đang khởi tạo dịch vụ: BGE-M3 (Tuần 3)...")
bge_m3_service = BgeM3EmbeddingService()

print("Đang khởi tạo dịch vụ: PhoBERT-Base (Tuần 4)...")
phobert_service = PhoBertEmbeddingService()
print("Tất cả các mô hình AI đã nạp thành công!")

class EmbedRequest(BaseModel):
    texts: List[str]
    model_name: Optional[str] = "E5_BASE"

@app.post("/embed")
async def get_embeddings(request: EmbedRequest):
    try:
        if not request.texts:
            raise HTTPException(status_code=400, detail="Danh sách dữ liệu 'texts' không được trống")
        
        # Chuẩn hóa tên mô hình truyền lên từ client
        selected_model = request.model_name.upper().strip() if request.model_name else "E5_BASE"
        
        # ĐIỀU PHỐI QUA FACTORY THEO MODEL_NAME
        if selected_model == "PHOBERT_BASE":
            return phobert_service.get_embeddings(request.texts)
        elif selected_model == "BGE_M3":
            return bge_m3_service.get_embeddings(request.texts)
        elif selected_model == "E5_BASE":
            embeddings = default_model.encode(request.texts)
            return embeddings.tolist()
        else:
            raise HTTPException(status_code=400, detail=f"Hệ thống chưa hỗ trợ dòng mô hình nhúng: {request.model_name}")
            
    except HTTPException as http_ex:
        raise http_ex
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống xử lý AI: {str(e)}")

@app.get("/health")
async def health_check():
    return {"status": "healthy", "week": 4, "supported_models": ["E5_BASE", "BGE_M3", "PHOBERT_BASE"]}

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)