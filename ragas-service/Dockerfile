FROM python:3.12-slim
WORKDIR /app

COPY requirements.txt .

RUN pip install --upgrade pip setuptools wheel && \
    pip install --no-cache-dir --retries 5 --timeout 120 -r requirements.txt

COPY main.py .

ENV RAGAS_MODEL=gemini-2.5-flash

ENV GEMINI_EMBEDDING_MODEL=models/gemini-embedding-001
ENV RAGAS_GEMINI_EMBEDDING_MODEL=models/gemini-embedding-001
ENV RAGAS_GEMINI_EMBEDDING_DIM=768

ENV RAGAS_OLLAMA_BASE_URL=http://ollama:11434
ENV RAGAS_OLLAMA_MODEL=qwen2.5:1.5b-instruct
ENV RAGAS_OLLAMA_EMBEDDING_MODEL=embeddinggemma

ENV RAGAS_MAX_WORKERS=1
ENV GEMINI_MAX_RETRIES=4

EXPOSE 8002

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8002", "--loop", "asyncio"]