"""
ragas-service — W4-14
=====================
FastAPI service tính 4 RAGAS metrics thực sự bằng thư viện ragas 0.2+.

LLM judge: Google Gemini (gemini-2.0-flash) — cùng model với phần backend Java.
Metrics:
  - Faithfulness           : answer có dựa trên context không? (hallucination check)
  - AnswerRelevancy        : answer có trả lời đúng question không?
  - ContextPrecision       : context retrieved có signal/noise tốt không?
  - ContextRecall          : context có đủ thông tin để trả lời ground_truth không?

Cách dùng:
  POST /evaluate
  Body: { question, answer, ground_truth, contexts: [str] }
  Response: { faithfulness, answer_relevancy, context_precision, context_recall }

Env vars:
  GOOGLE_API_KEY  — bắt buộc, dùng cho Gemini judge
  RAGAS_MODEL     — tùy chọn, mặc định "gemini-2.0-flash"
"""

import asyncio
import logging
import os
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

# ── ragas imports ──────────────────────────────────────────────────────────────
from ragas import SingleTurnSample
from ragas.metrics import (
    Faithfulness,
    AnswerRelevancy,
    LLMContextPrecisionWithReference,
    LLMContextRecall,
)
from ragas.llms import llm_factory

logger = logging.getLogger("ragas-service")
logging.basicConfig(level=logging.INFO, format="%(asctime)s  %(levelname)s  %(message)s")


# ── Global metric instances (shared, thread-safe reads) ────────────────────────
_faithfulness_metric: Optional[Faithfulness] = None
_answer_relevancy_metric: Optional[AnswerRelevancy] = None
_context_precision_metric: Optional[LLMContextPrecisionWithReference] = None
_context_recall_metric: Optional[LLMContextRecall] = None


def _init_metrics() -> None:
    """
    Khởi tạo ragas metric objects với Gemini judge.
    Được gọi một lần lúc startup.
    """
    global _faithfulness_metric, _answer_relevancy_metric
    global _context_precision_metric, _context_recall_metric

    api_key = os.environ.get("GOOGLE_API_KEY", "")
    if not api_key:
        logger.warning(
            "GOOGLE_API_KEY không được set. "
            "Các metric LLM-judge sẽ thất bại khi gọi thực sự."
        )

    model_name = os.environ.get("RAGAS_MODEL", "gemini-2.0-flash")
    logger.info("Khởi tạo Gemini judge: model=%s", model_name)

    # Tạo LLM judge qua ragas llm_factory với google-genai backend
    try:
        from google import genai  # noqa: PLC0415

        client = genai.Client(api_key=api_key)
        evaluator_llm = llm_factory(model_name, provider="google", client=client)
        logger.info("Đã tạo evaluator_llm thành công qua google-genai")
    except Exception as exc:  # fallback: OpenAI-compatible
        logger.warning("Không thể dùng google-genai (%s), thử LangchainLLMWrapper...", exc)
        from langchain_google_genai import ChatGoogleGenerativeAI  # noqa: PLC0415
        from ragas.llms import LangchainLLMWrapper  # noqa: PLC0415

        evaluator_llm = LangchainLLMWrapper(
            ChatGoogleGenerativeAI(model=model_name, google_api_key=api_key)
        )
        logger.info("Đã tạo evaluator_llm qua LangchainLLMWrapper")

    _faithfulness_metric = Faithfulness(llm=evaluator_llm)
    _answer_relevancy_metric = AnswerRelevancy(llm=evaluator_llm)
    _context_precision_metric = LLMContextPrecisionWithReference(llm=evaluator_llm)
    _context_recall_metric = LLMContextRecall(llm=evaluator_llm)

    logger.info("Tất cả 4 ragas metrics đã sẵn sàng.")


# ── FastAPI lifespan ───────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(_: FastAPI):
    """Khởi tạo metrics ở startup, không làm gì ở shutdown."""
    _init_metrics()
    yield


app = FastAPI(
    title="RAGAS Service",
    description="Tính 4 RAGAS metrics (faithfulness, answer_relevancy, context_precision, context_recall) dùng Gemini judge.",
    version="1.0.0",
    lifespan=lifespan,
)


# ── Pydantic schemas ───────────────────────────────────────────────────────────
class EvaluationRequest(BaseModel):
    question: str = Field(..., min_length=1, description="Câu hỏi gốc của người dùng")
    answer: str = Field(..., min_length=1, description="Câu trả lời do LLM sinh ra")
    ground_truth: str = Field(..., min_length=1, description="Câu trả lời đúng (ground truth)")
    contexts: list[str] = Field(default=[], description="Danh sách các đoạn context được retrieve")


class EvaluationResponse(BaseModel):
    faithfulness: float = Field(description="[0,1] Answer có dựa trên context không")
    answer_relevancy: float = Field(description="[0,1] Answer có liên quan đến question không")
    context_precision: float = Field(description="[0,1] Context retrieved có signal/noise tốt không")
    context_recall: float = Field(description="[0,1] Context có đủ thông tin để trả lời ground_truth không")


# ── Endpoints ──────────────────────────────────────────────────────────────────
@app.get("/health")
def health() -> dict[str, str]:
    """Health check — luôn trả ok nếu service đang chạy."""
    return {"status": "ok"}


@app.post("/evaluate", response_model=EvaluationResponse)
async def evaluate(req: EvaluationRequest) -> EvaluationResponse:
    """
    Tính 4 RAGAS metrics cho một cặp (question, answer, contexts, ground_truth).

    Mỗi metric được tính song song bằng asyncio.gather để tối ưu latency.
    Nếu một metric thất bại (ví dụ: API rate limit), trả về -1.0 cho metric đó
    và log cảnh báo thay vì fail toàn bộ request.
    """
    if _faithfulness_metric is None:
        raise HTTPException(status_code=503, detail="RAGAS metrics chưa được khởi tạo.")

    # Tạo 2 sample object riêng:
    #  - sample_with_ref: dùng cho ContextPrecision và ContextRecall (cần reference/ground_truth)
    #  - sample_no_ref  : dùng cho Faithfulness và AnswerRelevancy (không cần reference)
    sample_no_ref = SingleTurnSample(
        user_input=req.question,
        response=req.answer,
        retrieved_contexts=req.contexts,
    )

    sample_with_ref = SingleTurnSample(
        user_input=req.question,
        response=req.answer,
        retrieved_contexts=req.contexts,
        reference=req.ground_truth,
    )

    async def safe_score(metric, sample, metric_name: str) -> float:
        """Tính score một metric, trả -1.0 nếu thất bại."""
        try:
            score = await metric.single_turn_ascore(sample)
            # ragas trả về numpy float hoặc None trong một số trường hợp
            return float(score) if score is not None else 0.0
        except Exception as exc:
            logger.warning("Metric '%s' thất bại: %s", metric_name, exc)
            return -1.0

    # Chạy song song 4 metrics
    faithfulness_score, answer_relevancy_score, context_precision_score, context_recall_score = (
        await asyncio.gather(
            safe_score(_faithfulness_metric, sample_no_ref, "faithfulness"),
            safe_score(_answer_relevancy_metric, sample_no_ref, "answer_relevancy"),
            safe_score(_context_precision_metric, sample_with_ref, "context_precision"),
            safe_score(_context_recall_metric, sample_with_ref, "context_recall"),
        )
    )

    logger.info(
        "Evaluated | faithfulness=%.3f | answer_relevancy=%.3f "
        "| context_precision=%.3f | context_recall=%.3f",
        faithfulness_score,
        answer_relevancy_score,
        context_precision_score,
        context_recall_score,
    )

    return EvaluationResponse(
        faithfulness=faithfulness_score,
        answer_relevancy=answer_relevancy_score,
        context_precision=context_precision_score,
        context_recall=context_recall_score,
    )
