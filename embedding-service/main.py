from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from sentence_transformers import SentenceTransformer
import uvicorn

# [W3-14] Import service BgeM3 do Gia Bảo cài đặt
from bge_m3_embedding_service import BgeM3EmbeddingService

# Khởi tạo FastAPI
app = FastAPI(
    title="Embedding Service - Week 3",
    description="API chạy đa mô hình nhúng (E5-Base & BGE-M3)",
    version="3.0.0"
)

# --- KHỞI TẠO CÁC MÔ HÌNH NHÚNG ---
# 1. Mô hình cũ tuần 2 (Mặc định)
print("Đang tải mô hình chính multilingual-e5-base... Vui lòng đợi...")
default_model = SentenceTransformer('intfloat/multilingual-e5-base')
print("Tải mô hình mặc định thành công!")

# 2. [W3-14] Khởi tạo BgeM3EmbeddingService (Chỉ load mô hình khi hệ thống chạy lên)
print("Đang khởi tạo dịch vụ BGE-M3 Embedding Service...")
bge_m3_service = BgeM3EmbeddingService()
print("Khởi tạo dịch vụ BGE-M3 thành công!")


# Cấu trúc dữ liệu đầu vào (Bổ sung thêm tham số model_name để làm Factory phân tách)
class EmbedRequest(BaseModel):
    texts: List[str]
    model_name: Optional[str] = "E5_BASE"  # Nếu không truyền, mặc định chạy mô hình cũ


# API Endpoint xử lý trích xuất Vector theo cấu trúc Factory điều kiện
@app.post("/embed")
async def get_embeddings(request: EmbedRequest):
    """
    API tiếp nhận danh sách văn bản và tên mô hình nhúng, 
    chạy qua Factory để điều phối và trả về danh sách vector số thực sự.
    """
    try:
        if not request.texts:
            raise HTTPException(status_code=400, detail="Danh sách 'texts' không được trống")
        
        # --- [W3-14] ĐÓNG VAI TRÒ LÀM EMBEDDING SERVICE FACTORY ---
        selected_model = request.model_name.upper().strip() if request.model_name else "E5_BASE"
        
        if selected_model == "BGE_M3":
            # Gọi service xử lý trích xuất vector 1024-dim của Bảo
            return bge_m3_service.get_embeddings(request.texts)
            
        elif selected_model == "E5_BASE":
            # Chạy mô hình mặc định tuần 2 của nhóm
            embeddings = default_model.encode(request.texts)
            return embeddings.tolist()
            
        else:
            raise HTTPException(
                status_code=400, 
                detail=f"Hệ thống chưa hỗ trợ dòng mô hình nhúng: {request.model_name}"
            )
            
    except HTTPException as http_ex:
        raise http_ex
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý mô hình AI: {str(e)}")


@app.get("/health")
async def health_check():
    return {
        "status": "healthy", 
        "week": 3, 
        "supported_models": ["E5_BASE", "BGE_M3"]
    }

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)