from typing import List
from sentence_transformers import SentenceTransformer

class PhoBertEmbeddingService:
    def __init__(self):
        
        self.model_name = "vinai/phobert-base"
        self.model = SentenceTransformer(self.model_name)
        self.dimension = 768

    def get_embedding(self, text: str) -> List[float]:
        if not text or not text.strip():
            return []
        embedding = self.model.encode(text, normalize_embeddings=True)
        return embedding.tolist()

    def get_embeddings(self, texts: List[str]) -> List[List[float]]:
        if not texts:
            return []
        embeddings = self.model.encode(texts, normalize_embeddings=True)
        return embeddings.tolist()