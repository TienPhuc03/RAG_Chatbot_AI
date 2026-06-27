FROM python:3.12-slim
WORKDIR /app

# Cài dependencies trước (layer cache tốt hơn)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .

# GOOGLE_API_KEY phải được truyền vào lúc docker run hoặc qua docker-compose env
ENV RAGAS_MODEL=gemini-2.0-flash

EXPOSE 8002

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8002"]
