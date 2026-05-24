from fastapi import FastAPI
from pydantic import BaseModel


class EmbeddingRequest(BaseModel):
    texts: list[str]


app = FastAPI(title="Embedding Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/embed")
def embed(request: EmbeddingRequest) -> dict[str, list[list[float]]]:
    embeddings = [[0.0, 0.0, 0.0] for _ in request.texts]
    return {"embeddings": embeddings}
