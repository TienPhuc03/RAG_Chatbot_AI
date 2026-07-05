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
    version="4.1.0"
)

print("Đang tải mô hình mặc định: multilingual-e5-base (Tuần 2)...")
default_model = SentenceTransformer("intfloat/multilingual-e5-base")

print("Đang khởi tạo dịch vụ: BGE-M3 (Tuần 3)...")
bge_m3_service = BgeM3EmbeddingService()

print("Đang khởi tạo dịch vụ: PhoBERT-Base (Tuần 4)...")
phobert_service = PhoBertEmbeddingService()

print("Tất cả các mô hình AI đã nạp thành công!")


class EmbedRequest(BaseModel):
    texts: List[str]

    # Java backend hiện gửi field "model".
    # Các test cũ hoặc client cũ có thể vẫn gửi "model_name".
    # Vì vậy service nhận cả hai để tránh route nhầm về E5_BASE.
    model: Optional[str] = None
    model_name: Optional[str] = None


MODEL_ALIASES = {
    # BGE-M3: vector dimension 1024
    "bge_m3": "BGE_M3",
    "bge-m3": "BGE_M3",
    "baai/bge-m3": "BGE_M3",

    # multilingual-e5-base: vector dimension 768
    "e5_base": "E5_BASE",
    "e5-base": "E5_BASE",
    "multilingual_e5_base": "E5_BASE",
    "multilingual-e5-base": "E5_BASE",
    "intfloat/multilingual-e5-base": "E5_BASE",

    # PhoBERT-base: vector dimension 768
    "phobert_base": "PHOBERT_BASE",
    "phobert-base": "PHOBERT_BASE",
    "vinai/phobert-base": "PHOBERT_BASE",
}


def resolve_model_name(request: EmbedRequest) -> str:
    raw_model = request.model or request.model_name or "E5_BASE"
    normalized = raw_model.strip().lower()
    normalized = normalized.replace(" ", "_")

    selected_model = MODEL_ALIASES.get(normalized)
    if selected_model is None:
        raise HTTPException(
            status_code=400,
            detail=f"Hệ thống chưa hỗ trợ dòng mô hình nhúng: {raw_model}"
        )

    return selected_model


@app.post("/embed")
async def get_embeddings(request: EmbedRequest):
    try:
        if not request.texts:
            raise HTTPException(
                status_code=400,
                detail="Danh sách dữ liệu 'texts' không được trống"
            )

        selected_model = resolve_model_name(request)

        if selected_model == "BGE_M3":
            embeddings = bge_m3_service.get_embeddings(request.texts)
        elif selected_model == "PHOBERT_BASE":
            embeddings = phobert_service.get_embeddings(request.texts)
        elif selected_model == "E5_BASE":
            embeddings = default_model.encode(request.texts).tolist()
        else:
            raise HTTPException(
                status_code=400,
                detail=f"Hệ thống chưa hỗ trợ dòng mô hình nhúng: {selected_model}"
            )

        # Debug nhẹ để xác nhận route đúng model và đúng dimension.
        vector_dim = len(embeddings[0]) if embeddings else 0
        print(f"REQUEST MODEL = {request.model or request.model_name}")
        print(f"RESOLVED MODEL = {selected_model}")
        print(f"VECTOR DIM = {vector_dim}")

        return embeddings

    except HTTPException as http_ex:
        raise http_ex
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống xử lý AI: {str(e)}")


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "week": 4,
        "supported_models": [
            "E5_BASE",
            "MULTILINGUAL_E5_BASE",
            "BGE_M3",
            "PHOBERT_BASE",
            "multilingual-e5-base",
            "bge-m3",
            "phobert-base"
        ]
    }


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)