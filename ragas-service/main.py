from __future__ import annotations

from collections import Counter
from math import sqrt
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, ConfigDict, Field

try:
    from datasets import Dataset
    from ragas import evaluate as ragas_evaluate
    from ragas.metrics import answer_relevancy, context_precision, context_recall, faithfulness

    RAGAS_AVAILABLE = True
except Exception:
    RAGAS_AVAILABLE = False


class EvaluationRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    question: str
    answer: str
    ground_truth: str = Field(alias="groundTruth")
    contexts: list[str] = Field(default_factory=list)


app = FastAPI(title="RAGAS Service", version="0.2.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/evaluate")
def evaluate(payload: EvaluationRequest) -> dict[str, Any]:
    try:
        metrics = evaluate_with_ragas(payload)
        source = "ragas"
    except Exception:
        metrics = local_fallback_metrics(payload)
        source = "local-fallback"

    return {
        "faithfulness": metrics["faithfulness"],
        "answerRelevancy": metrics["answer_relevancy"],
        "contextPrecision": metrics["context_precision"],
        "contextRecall": metrics["context_recall"],
        "source": source,
    }


def evaluate_with_ragas(payload: EvaluationRequest) -> dict[str, float]:
    if not RAGAS_AVAILABLE:
        raise RuntimeError("ragas package is not available")

    dataset = Dataset.from_dict(
        {
            "question": [payload.question],
            "answer": [payload.answer],
            "ground_truth": [payload.ground_truth],
            "contexts": [payload.contexts],
        }
    )

    result = ragas_evaluate(
        dataset,
        metrics=[faithfulness, answer_relevancy, context_precision, context_recall],
    )

    return extract_metrics(result)


def extract_metrics(result: Any) -> dict[str, float]:
    if hasattr(result, "to_pandas"):
        frame = result.to_pandas()
        row = frame.iloc[0].to_dict()
        return {
            "faithfulness": float(row.get("faithfulness", 0.0) or 0.0),
            "answer_relevancy": float(row.get("answer_relevancy", 0.0) or 0.0),
            "context_precision": float(row.get("context_precision", 0.0) or 0.0),
            "context_recall": float(row.get("context_recall", 0.0) or 0.0),
        }

    if isinstance(result, dict):
        return {
            "faithfulness": float(result.get("faithfulness", 0.0) or 0.0),
            "answer_relevancy": float(result.get("answer_relevancy", 0.0) or 0.0),
            "context_precision": float(result.get("context_precision", 0.0) or 0.0),
            "context_recall": float(result.get("context_recall", 0.0) or 0.0),
        }

    scores = getattr(result, "scores", None)
    if scores is not None:
        try:
            first = scores[0]
            return {
                "faithfulness": float(getattr(first, "faithfulness", 0.0) or 0.0),
                "answer_relevancy": float(getattr(first, "answer_relevancy", 0.0) or 0.0),
                "context_precision": float(getattr(first, "context_precision", 0.0) or 0.0),
                "context_recall": float(getattr(first, "context_recall", 0.0) or 0.0),
            }
        except Exception:
            pass

    raise RuntimeError("Unable to extract ragas metrics")


def local_fallback_metrics(payload: EvaluationRequest) -> dict[str, float]:
    truth_tokens = tokenize(payload.ground_truth)
    answer_tokens = tokenize(payload.answer)

    exact_match = 1.0 if normalize(payload.ground_truth) == normalize(payload.answer) else 0.0
    f1_score = f1(truth_tokens, answer_tokens)

    context_precision = context_overlap_precision(payload.contexts, payload.ground_truth)
    context_recall = context_overlap_recall(payload.contexts, payload.ground_truth)

    faithfulness = min(1.0, max(0.0, 0.5 * exact_match + 0.5 * context_precision))
    answer_relevancy = min(1.0, max(0.0, f1_score))

    return {
        "faithfulness": faithfulness,
        "answer_relevancy": answer_relevancy,
        "context_precision": context_precision,
        "context_recall": context_recall,
    }


def tokenize(text: str) -> list[str]:
    return [token for token in normalize(text).split() if token]


def normalize(text: str) -> str:
    return "".join(character.lower() if character.isalnum() or character.isspace() else " " for character in text).strip()


def f1(truth_tokens: list[str], answer_tokens: list[str]) -> float:
    if not truth_tokens or not answer_tokens:
        return 0.0
    truth_counts = Counter(truth_tokens)
    answer_counts = Counter(answer_tokens)
    common = sum((truth_counts & answer_counts).values())
    if common == 0:
        return 0.0
    precision = common / len(answer_tokens)
    recall = common / len(truth_tokens)
    return (2 * precision * recall) / (precision + recall)


def context_overlap_precision(contexts: list[str], ground_truth: str) -> float:
    if not contexts:
        return 0.0
    truth_tokens = set(tokenize(ground_truth))
    if not truth_tokens:
        return 0.0
    context_tokens = set()
    for context in contexts:
        context_tokens.update(tokenize(context))
    if not context_tokens:
        return 0.0
    overlap = len(context_tokens & truth_tokens)
    return overlap / len(context_tokens)


def context_overlap_recall(contexts: list[str], ground_truth: str) -> float:
    if not contexts:
        return 0.0
    truth_tokens = set(tokenize(ground_truth))
    if not truth_tokens:
        return 0.0
    context_tokens = set()
    for context in contexts:
        context_tokens.update(tokenize(context))
    if not context_tokens:
        return 0.0
    overlap = len(context_tokens & truth_tokens)
    return overlap / len(truth_tokens)
