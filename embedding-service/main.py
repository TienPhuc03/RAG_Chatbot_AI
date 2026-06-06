from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List

# Khởi tạo ứng dụng FastAPI đúng chuẩn đặc tả của nhóm
app = FastAPI(
    title="Embedding Service - Week 1",
    description="API stub phục vụ kết nối hệ thống và test contract tuần 1",
    version="1.0.0"
)

# Cấu trúc dữ liệu đầu vào (Request Body) nhận một danh sách các câu văn
class EmbedRequest(BaseModel):
    texts: List[str]

# 1. API Endpoint xử lý đơn lẻ hoặc hàng loạt (gộp chung /embed theo thiết kế FastAPI thông thường)
@app.post("/embed")
async def get_embeddings(request: EmbedRequest):
    """
    API tiếp nhận danh sách văn bản và trả về danh sách các vector giả lập (stub 0.0)
    Đầu vào: {"texts": ["câu 1", "câu 2"]} -> Đầu ra: [[0.0, ...], [0.0, ...]]
    """
    try:
        if not request.texts:
            raise HTTPException(status_code=400, detail="Danh sách văn bản 'texts' không được để trống")
        
        # Tạo vector giả lập stub 0.0 cố định gồm 5 phần tử để giữ cấu trúc mảng 2 chiều List[List[float]]
        stub_vector = [0.0, 0.0, 0.0, 0.0, 0.0]
        
        # Duyệt qua từng câu văn trong request để trả về số lượng vector tương ứng
        result = [stub_vector for _ in request.texts]
        return result
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Lỗi xử lý hệ thống: {str(e)}")

# 2. API Endpoint /embed-batch (Viết riêng một router nữa để khớp hoàn toàn 100% với Deliverable trên Jira)
@app.post("/embed-batch")
async def get_embeddings_batch(request: EmbedRequest):
    """
    API phục vụ xử lý danh sách lớn (batch) theo đúng task W1-21
    """
    return await get_embeddings(request)

# 3. API Endpoint kiểm tra trạng thái hoạt động (Health Check)
@app.get("/health")
async def health_check():
    """
    API thông báo trạng thái hoạt động của dịch vụ nhúng
    """
    return {
        "status": "healthy",
        "week": 1,
        "owner": "Gia Bảo"
    }