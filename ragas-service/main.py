from fastapi import FastAPI
from pydantic import BaseModel


class EvaluationRequest(BaseModel):
    question: str
    answer: str
    ground_truth: str
    contexts: list[str] = []


app = FastAPI(title="RAGAS Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/evaluate")
def evaluate(_: EvaluationRequest) -> dict[str, float]:
    return {
        "faithfulness": 0.0,
        "answer_relevancy": 0.0,
        "context_precision": 0.0,
        "context_recall": 0.0,
    }
