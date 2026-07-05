import asyncio
import logging
import os
from contextlib import asynccontextmanager
from dataclasses import dataclass
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, ConfigDict, Field
from ragas import SingleTurnSample
from ragas.llms import LangchainLLMWrapper
from ragas.metrics import (
    AnswerRelevancy,
    Faithfulness,
    LLMContextPrecisionWithReference,
    LLMContextRecall,
)


logger = logging.getLogger("ragas-service")
logging.basicConfig(level=logging.INFO, format="%(asctime)s  %(levelname)s  %(message)s")

PRIMARY_PROVIDER = "GEMINI"
FALLBACK_PROVIDER = "OLLAMA"

DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
DEFAULT_GEMINI_EMBEDDING_MODEL = "models/gemini-embedding-001"
DEFAULT_GEMINI_EMBEDDING_DIM = 768

DEFAULT_OLLAMA_MODEL = "qwen2.5:1.5b-instruct"
DEFAULT_OLLAMA_EMBEDDING_MODEL = "embeddinggemma"
DEFAULT_OLLAMA_BASE_URL = "http://ollama:11434"


@dataclass(frozen=True)
class MetricBundle:
    provider: str
    faithfulness: Faithfulness
    answer_relevancy: AnswerRelevancy
    context_precision: LLMContextPrecisionWithReference
    context_recall: LLMContextRecall


class JudgeQuotaError(RuntimeError):
    pass


class MetricEvaluationError(RuntimeError):
    pass


_google_metrics: Optional[MetricBundle] = None
_ollama_metrics: Optional[MetricBundle] = None
_google_init_error: Optional[str] = None
_ollama_init_error: Optional[str] = None


def _env(name: str, default: str) -> str:
    return os.environ.get(name, default).strip() or default


def _env_int(name: str, default: int) -> int:
    value = os.environ.get(name, "").strip()
    if not value:
        return default

    try:
        return int(value)
    except ValueError:
        logger.warning("Invalid integer env %s=%s. Using default=%s", name, value, default)
        return default


def _build_metric_bundle(provider: str, evaluator_llm, evaluator_embeddings) -> MetricBundle:
    return MetricBundle(
        provider=provider,
        faithfulness=Faithfulness(llm=evaluator_llm),
        answer_relevancy=AnswerRelevancy(llm=evaluator_llm, embeddings=evaluator_embeddings),
        context_precision=LLMContextPrecisionWithReference(llm=evaluator_llm),
        context_recall=LLMContextRecall(llm=evaluator_llm),
    )


def _gemini_api_key() -> str:
    return (
        os.environ.get("GOOGLE_API_KEY", "").strip()
        or os.environ.get("GEMINI_API_KEY", "").strip()
    )


def _gemini_model_name() -> str:
    return _env("RAGAS_MODEL", DEFAULT_GEMINI_MODEL)

def _gemini_embedding_model_name() -> str:
    # Với langchain-google-genai/google SDK version hiện tại của project,
    # BatchEmbedContents cần format: models/gemini-embedding-001
    model = (
        os.environ.get("RAGAS_GEMINI_EMBEDDING_MODEL", "").strip()
        or os.environ.get("GEMINI_EMBEDDING_MODEL", "").strip()
        or DEFAULT_GEMINI_EMBEDDING_MODEL
    )

    if not model.startswith("models/"):
        model = f"models/{model}"

    return model

def _gemini_embedding_dim() -> int:
    return _env_int("RAGAS_GEMINI_EMBEDDING_DIM", DEFAULT_GEMINI_EMBEDDING_DIM)


def _ollama_model_name() -> str:
    return _env("RAGAS_OLLAMA_MODEL", DEFAULT_OLLAMA_MODEL)


def _ollama_embedding_model_name() -> str:
    return _env("RAGAS_OLLAMA_EMBEDDING_MODEL", DEFAULT_OLLAMA_EMBEDDING_MODEL)


def _ollama_base_url() -> str:
    return _env("RAGAS_OLLAMA_BASE_URL", DEFAULT_OLLAMA_BASE_URL)


def _build_google_metrics() -> MetricBundle:
    api_key = _gemini_api_key()
    if not api_key:
        raise ValueError("GOOGLE_API_KEY or GEMINI_API_KEY is missing")

    model_name = _gemini_model_name()
    embedding_model_name = _gemini_embedding_model_name()
    embedding_dim = _gemini_embedding_dim()
    max_retries = _env_int("GEMINI_MAX_RETRIES", 4)

    try:
        from langchain_google_genai import ChatGoogleGenerativeAI  # noqa: PLC0415
        from langchain_google_genai import GoogleGenerativeAIEmbeddings  # noqa: PLC0415
        from ragas.embeddings import LangchainEmbeddingsWrapper  # noqa: PLC0415

        evaluator_llm = LangchainLLMWrapper(
            ChatGoogleGenerativeAI(
                model=model_name,
                google_api_key=api_key,
                temperature=0,
                api_version="v1",
                max_retries=max_retries,
            )
        )

        evaluator_embeddings = LangchainEmbeddingsWrapper(
            GoogleGenerativeAIEmbeddings(
                model=embedding_model_name,
                google_api_key=api_key,
                api_version="v1",
                output_dimensionality=embedding_dim,
            )
        )

        logger.info(
            "Initialized Gemini judge via LangChain wrapper successfully: model=%s",
            model_name,
        )
        logger.info(
            "Initialized Gemini embeddings successfully: model=%s, dim=%s",
            embedding_model_name,
            embedding_dim,
        )

        return _build_metric_bundle(PRIMARY_PROVIDER, evaluator_llm, evaluator_embeddings)

    except Exception as exc:
        logger.error("Critical: Google metrics initialization failed: %s", exc)
        raise


def _build_ollama_metrics() -> MetricBundle:
    from langchain_ollama import ChatOllama, OllamaEmbeddings  # noqa: PLC0415
    from ragas.embeddings import LangchainEmbeddingsWrapper  # noqa: PLC0415

    base_url = _ollama_base_url()

    # Model này chỉ dùng làm judge/chat.
    model_name = _ollama_model_name()

    # Model này mới dùng để embedding.
    embedding_model_name = _ollama_embedding_model_name()

    evaluator_llm = LangchainLLMWrapper(
        ChatOllama(
            model=model_name,
            base_url=base_url,
            temperature=0,
        )
    )

    evaluator_embeddings = LangchainEmbeddingsWrapper(
        OllamaEmbeddings(
            model=embedding_model_name,
            base_url=base_url,
        )
    )

    logger.info("Initialized Ollama judge: model=%s, base_url=%s", model_name, base_url)
    logger.info(
        "Initialized Ollama embeddings: model=%s, base_url=%s",
        embedding_model_name,
        base_url,
    )

    return _build_metric_bundle(FALLBACK_PROVIDER, evaluator_llm, evaluator_embeddings)


def _init_metrics() -> None:
    global _google_metrics, _ollama_metrics, _google_init_error, _ollama_init_error

    _google_metrics = None
    _ollama_metrics = None
    _google_init_error = None
    _ollama_init_error = None

    try:
        _google_metrics = _build_google_metrics()
    except Exception as exc:
        _google_init_error = str(exc)
        logger.warning("Gemini judge unavailable at startup: %s", exc)

    try:
        _ollama_metrics = _build_ollama_metrics()
    except Exception as exc:
        _ollama_init_error = str(exc)
        logger.warning("Ollama judge unavailable at startup: %s", exc)

    logger.info(
        "Judge readiness | google=%s | ollama=%s",
        _google_metrics is not None,
        _ollama_metrics is not None,
    )


def _is_google_quota_error(exc: Exception) -> bool:
    message = str(exc).lower()

    # 404 / not found là lỗi cấu hình model, không phải quota/rate-limit.
    # Không fallback trong trường hợp này để tránh che lỗi thật.
    if "404" in message or "not_found" in message or "not found" in message:
        return False

    return (
        "quota exceeded" in message
        or "rate limit" in message
        or "resource exhausted" in message
        or "status code 429" in message
        or " 429 " in message
        or "503" in message
        or "unavailable" in message
        or "overloaded" in message
        or "timeout" in message
        or "deadline exceeded" in message
    )


async def _score_bundle(
    bundle: MetricBundle,
    sample_no_ref: SingleTurnSample,
    sample_with_ref: SingleTurnSample,
    fail_fast_on_google_quota: bool,
) -> tuple[float, float, float, float]:
    async def safe_score(metric, sample, metric_name: str) -> float:
        try:
            score = await metric.single_turn_ascore(sample)
            return float(score) if score is not None else 0.0
        except Exception as exc:
            if (
                fail_fast_on_google_quota
                and bundle.provider == PRIMARY_PROVIDER
                and _is_google_quota_error(exc)
            ):
                raise JudgeQuotaError(str(exc)) from exc

            logger.warning(
                "Metric failed | provider=%s | metric=%s | error=%s",
                bundle.provider,
                metric_name,
                exc,
            )

            # Không trả -1.0 nữa vì -1.0 không phải điểm RAGAS thật.
            raise MetricEvaluationError(
                f"Metric failed | provider={bundle.provider} | metric={metric_name} | error={exc}"
            ) from exc

    scores = await asyncio.gather(
        safe_score(bundle.faithfulness, sample_no_ref, "faithfulness"),
        safe_score(bundle.answer_relevancy, sample_no_ref, "answer_relevancy"),
        safe_score(bundle.context_precision, sample_with_ref, "context_precision"),
        safe_score(bundle.context_recall, sample_with_ref, "context_recall"),
    )

    return (
        scores[0],
        scores[1],
        scores[2],
        scores[3],
    )


async def _evaluate_with_bundle(
    bundle: MetricBundle,
    sample_no_ref: SingleTurnSample,
    sample_with_ref: SingleTurnSample,
    judge_fallback_used: bool,
    fail_fast_on_google_quota: bool,
) -> "EvaluationResponse":
    faithfulness_score, answer_relevancy_score, context_precision_score, context_recall_score = (
        await _score_bundle(
            bundle,
            sample_no_ref,
            sample_with_ref,
            fail_fast_on_google_quota=fail_fast_on_google_quota,
        )
    )

    logger.info(
        "Evaluated | provider=%s | faithfulness=%.3f | answer_relevancy=%.3f | context_precision=%.3f | context_recall=%.3f",
        bundle.provider,
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
        judge_provider=bundle.provider,
        judge_fallback_used=judge_fallback_used,
    )


@asynccontextmanager
async def lifespan(_: FastAPI):
    _init_metrics()
    yield


app = FastAPI(
    title="RAGAS Service",
    description=(
        "Compute 4 RAGAS metrics: faithfulness, answer_relevancy, "
        "context_precision, context_recall with Gemini primary and Ollama fallback."
    ),
    version="1.0.0",
    lifespan=lifespan,
)


class EvaluationRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    question: str = Field(..., min_length=1, description="Original user question")
    answer: str = Field(..., min_length=1, description="Model-generated answer")
    ground_truth: str = Field(
        ...,
        alias="groundTruth",
        min_length=1,
        description="Expected answer used as reference",
    )
    contexts: list[str] = Field(
        default_factory=list,
        description="Retrieved context chunks used to answer the question",
    )


class EvaluationResponse(BaseModel):
    faithfulness: float = Field(description="[0,1] Whether the answer is grounded in context")
    answer_relevancy: float = Field(
        serialization_alias="answerRelevancy",
        description="[0,1] Whether the answer stays on-topic for the question",
    )
    context_precision: float = Field(
        serialization_alias="contextPrecision",
        description="[0,1] Whether the retrieved context is high-signal",
    )
    context_recall: float = Field(
        serialization_alias="contextRecall",
        description="[0,1] Whether the context covers the reference answer",
    )
    judge_provider: str = Field(
        serialization_alias="judgeProvider",
        description="Judge provider that produced this score set",
    )
    judge_fallback_used: bool = Field(
        serialization_alias="judgeFallbackUsed",
        description="Whether Ollama fallback was used instead of Gemini",
    )


@app.get("/health")
def health() -> dict[str, object]:
    return {
        "status": "ok",
        "googleJudgeAvailable": _google_metrics is not None,
        "ollamaJudgeAvailable": _ollama_metrics is not None,
        "googleInitError": _google_init_error,
        "ollamaInitError": _ollama_init_error,
        "primaryProvider": PRIMARY_PROVIDER,
        "fallbackProvider": FALLBACK_PROVIDER,
        "geminiModel": _gemini_model_name(),
        "geminiEmbeddingModel": _gemini_embedding_model_name(),
        "geminiEmbeddingDim": _gemini_embedding_dim(),
        "ollamaModel": _ollama_model_name(),
        "ollamaEmbeddingModel": _ollama_embedding_model_name(),
        "ollamaBaseUrl": _ollama_base_url(),
    }


@app.post("/evaluate", response_model=EvaluationResponse)
async def evaluate(req: EvaluationRequest) -> EvaluationResponse:
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

    if _google_metrics is None and _ollama_metrics is None:
        raise HTTPException(
            status_code=503,
            detail={
                "message": "No judge provider is available. Check Gemini credentials and Ollama connectivity.",
                "googleInitError": _google_init_error,
                "ollamaInitError": _ollama_init_error,
            },
        )

    if _google_metrics is not None:
        try:
            return await _evaluate_with_bundle(
                _google_metrics,
                sample_no_ref,
                sample_with_ref,
                judge_fallback_used=False,
                fail_fast_on_google_quota=True,
            )
        except JudgeQuotaError as exc:
            logger.warning(
                "Gemini judge hit quota/rate-limit/service-unavailable. Falling back to Ollama. error=%s",
                exc,
            )
            if _ollama_metrics is None:
                raise HTTPException(
                    status_code=503,
                    detail="Gemini judge is unavailable/rate-limited and Ollama fallback is unavailable.",
                ) from exc
        except MetricEvaluationError as exc:
            # Lỗi cấu hình hoặc lỗi metric thật, không fallback để tránh che lỗi.
            raise HTTPException(
                status_code=502,
                detail=f"Gemini metric evaluation failed: {exc}",
            ) from exc

    if _ollama_metrics is None:
        raise HTTPException(
            status_code=503,
            detail="Gemini judge is unavailable and Ollama fallback is unavailable.",
        )

    try:
        return await _evaluate_with_bundle(
            _ollama_metrics,
            sample_no_ref,
            sample_with_ref,
            judge_fallback_used=True,
            fail_fast_on_google_quota=False,
        )
    except MetricEvaluationError as exc:
        raise HTTPException(
            status_code=502,
            detail=f"Ollama metric evaluation failed: {exc}",
        ) from exc