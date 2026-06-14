import os
from typing import List
from sentence_transformers import SentenceTransformer

class BgeM3EmbeddingService:
    def __init__(self):
        # Tải mô hình bge-m3 từ Hugging Face (Kích thước vector đầu ra dim=1024)
        # Mô hình này sẽ tự động tải về khi chạy service lần đầu tiên
        self.model_name = "BAAI/bge-m3"
        self.model = SentenceTransformer(self.model_name)
        self.dimension = 1024
        print(self.dimension)

    def get_embedding(self, text: str) -> List[float]:
        """Biến đổi một chuỗi văn bản đơn lẻ thành một vector embedding"""
        if not text or not text.strip():
            return []
        
        # normalize_embeddings=True để đảm bảo các vector trả về có độ dài chuẩn hóa
        embedding = self.model.encode(text, normalize_embeddings=True)
        return embedding.tolist()

    def get_embeddings(self, texts: List[str]) -> List[List[float]]:
        """Biến đổi một danh sách nhiều chuỗi văn bản thành danh sách các vector"""
        if not texts:
            return []
        
        embeddings = self.model.encode(texts, normalize_embeddings=True)
        return embeddings.tolist()