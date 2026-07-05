# RAG Chatbot — Hỏi đáp tài liệu môn học tiếng Việt

> **Đề tài:** Xây dựng chatbot cho phép sinh viên hỏi đáp dựa trên tài liệu môn học, đồng thời nghiên cứu và so sánh hiệu quả giữa **RAG (Retrieval-Augmented Generation)** và **fine-tuning** trong bối cảnh tiếng Việt.

## Mục lục

- [Research Questions](#research-questions)
- [Functional Requirements](#functional-requirements)
- [Deliverables](#deliverables)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Tech stack](#tech-stack)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Bắt đầu nhanh (Docker Compose)](#bắt-đầu-nhanh-docker-compose)
- [Chạy local không dùng Docker](#chạy-local-không-dùng-docker)
- [Biến môi trường](#biến-môi-trường)
- [API chính](#api-chính)
- [Fine-tuning (QLoRA)](#fine-tuning-qlora)
- [Nhóm thực hiện](#nhóm-thực-hiện)

---

## Research Questions

| # | Câu hỏi | Trạng thái đo lường |
|---|---|---|
| **RQ chính** | RAG hay fine-tuning hiệu quả hơn cho chatbot hỗ trợ học tập với tài liệu tiếng Việt, xét theo độ chính xác, chi phí triển khai và khả năng cập nhật kiến thức? | So sánh qua `ExperimentType.RAG` vs `ExperimentType.FINETUNE` trên cùng test set, có ước tính  `latency_ms` cho mỗi run |
| **RQ phụ 1** | Chunking strategy nào (fixed-size, semantic, hierarchical) cho retrieval accuracy cao nhất với slide bài giảng PDF? | Benchmark theo `ChunkingStrategy` (`FIXED_SIZE`, `SEMANTIC`, `HIERARCHICAL`), đo `retrieval_hit` + 4 chỉ số RAGAS |
| **RQ phụ 2** | Embedding model nào (multilingual-e5, PhoBERT, bge-m3, OpenAI/Gemini) phù hợp nhất cho tài liệu kỹ thuật tiếng Việt? | Benchmark theo `EmbeddingModel`; xem [Ghi chú embedding model](#ghi-chú-embedding-model) |

## Functional Requirements

### A. Tính năng hệ thống

**1. Quản lý tài liệu**
- [x] Upload PDF, DOCX, PPTX slide bài giảng (`POST /api/documents/upload`, parse bằng Apache Tika, tự phát hiện PDF scan không có text để báo lỗi rõ ràng).
- [x] Tự động chunk & embed tài liệu theo pipeline bất đồng bộ (`DocumentIndexingWorker`), theo dõi vòng đời `PENDING → PROCESSING → INDEXED / FAILED`.
- [x] Quản lý theo môn học / chương qua `courseCode` + `chapterCode` (demo mặc định môn `JAVA101`).
- [x] Xem danh sách tài liệu đã index, preview chunk (`GET /api/documents`, `GET /api/documents/{id}/chunks`), xóa tài liệu kèm dọn vector trong Qdrant.

**2. Chat & Hỏi đáp**
- [x] Chat tự nhiên theo ngữ cảnh hội thoại (giữ 5 lượt gần nhất làm history khi gọi LLM).
- [x] Trích dẫn nguồn tài liệu gốc (documentId, chunkId, tên file, số trang) trả kèm mỗi câu trả lời.
- [x] Giới hạn trả lời trong phạm vi tài liệu: prompt ràng buộc chỉ dùng ngữ cảnh retrieve được; nếu chưa có tài liệu `INDEXED` phù hợp, hệ thống từ chối trả lời và yêu cầu upload thêm.
- [x] Lịch sử hội thoại theo phiên (`sessionId`), hỗ trợ đính kèm file trực tiếp trong khung chat.

**3. Module nghiên cứu (RBL)**
- [x] So sánh RAG vs fine-tuned model trên cùng test set (`BenchmarkRunnerService`, `ExperimentType`).
- [x] Benchmark nhiều chunking strategy (Fixed-size, Semantic, Hierarchical).
- [x] Benchmark nhiều embedding model (xem bảng bên dưới).
- [x] Dashboard hiển thị kết quả thực nghiệm (`/benchmark` trên frontend, gọi `GET /api/benchmark/results`), tổng hợp trung bình theo nhóm chunking × embedding × experiment.

#### Ghi chú embedding model

| Model (đề bài) | Trạng thái trong hệ thống |
|---|---|
| `multilingual-e5-base` | Đang dùng — phục vụ qua `embedding-service` (FastAPI, `sentence-transformers`) |
| `PhoBERT-base` | Đang dùng — phục vụ qua `embedding-service` |
| `bge-m3` (BAAI) | Đang dùng — phục vụ qua `embedding-service` |
| `gemini-embedding-001` | Bổ sung ngoài đề bài — dùng làm embedding + LLM mặc định cho pipeline chat chính (qua Gemini API) |

### B. Sản phẩm bàn giao (Deliverables)

**1. Sản phẩm kỹ thuật**
- [x] Web app chatbot: backend Spring Boot (hexagonal architecture) + frontend React/Vite/Tailwind.
- [x] Source code trên GitHub kèm README (file này).
- [x] Test set 50 câu hỏi + ground truth: [`datasets/benchmark/test_set.json`](datasets/benchmark/test_set.json) (đồng bộ với `backend/src/main/resources/static/test-data/test_set.json`), phân loại `DEFINITION` / `COMPARISON` / `APPLICATION`.

**2. Sản phẩm nghiên cứu (RBL)**
- [ ] Báo cáo thực nghiệm so sánh models — *(soạn riêng ngoài repo, tổng hợp từ dữ liệu `GET /api/benchmark/results`)*.
- [x] Bảng số liệu RAGAS benchmark — sinh tự động, lưu trong bảng `benchmark_results` (Postgres, xem `V1__init_schema.sql`, `V3`, `V7`), truy vấn qua `BenchmarkResultRepository.findAverageMetricsByStrategyAndModel()`.

## Kiến trúc hệ thống

Backend áp dụng **hexagonal architecture (ports & adapters)**:

```
domain/        -> model, enum, port (interface) và exception thuần nghiệp vụ
application/   -> use case + DTO, điều phối luồng nghiệp vụ
infrastructure/-> adapter cụ thể: Gemini, Qdrant, Tika, Ollama, RAGAS client, JPA repository
api/           -> REST controller + xử lý lỗi tập trung
config/        -> Spring configuration, properties binding
```

```
┌────────────┐      ┌──────────────────┐      ┌─────────────┐
│  Frontend  │─────▶│  Spring Boot API  │─────▶│  PostgreSQL │
│ React+Vite │      │   (hexagonal)     │      │  + pgvector │
└────────────┘      └──────┬─────┬─────┘      └─────────────┘
                            │     │
              ┌─────────────┘     └─────────────┐
              ▼                                  ▼
      ┌───────────────┐                 ┌────────────────┐
      │  Qdrant (vector│                 │  Gemini API /  │
      │   store)       │                 │  Ollama local  │
      └───────────────┘                 └────────────────┘
              ▲
              │ embeddings (đa mô hình / benchmark)
      ┌───────────────┐        ┌───────────────────┐
      │ embedding-     │        │  ragas-service     │
      │ service (Py)   │        │  (Py, LLM-judge)   │
      │ e5 / PhoBERT / │        │  Gemini + Ollama    │
      │ BGE-M3         │        │  fallback           │
      └───────────────┘        └───────────────────┘
```

## Tech stack

| Thành phần | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Maven, hexagonal architecture |
| Frontend | React 19, Vite, Tailwind CSS 4, React Router |
| Cơ sở dữ liệu quan hệ | PostgreSQL 16 + pgvector (Flyway migration) |
| Vector store | Qdrant |
| LLM & Embedding chính | Gemini API (`gemini-2.5-flash`, `gemini-embedding-001`) qua `google-genai` SDK |
| LLM fallback | Ollama local (model fine-tune `hoc-phan-chatbot`) |
| Document parsing | Apache Tika |
| Chunking | Tự cài đặt: Fixed-size, Semantic (theo câu), Hierarchical (section → bullet/sentence) |
| Microservices Python | FastAPI — `embedding-service` (E5/PhoBERT/BGE-M3), `ragas-service` (RAGAS + Gemini/Ollama judge) |
| Fine-tuning | QLoRA (`peft`, `trl`, `bitsandbytes`) trên `Qwen2.5-1.5B-Instruct`, phục vụ qua Ollama |
| Testing | JUnit, Testcontainers (Postgres) |
| API docs | springdoc-openapi (Swagger UI) |

## Cấu trúc thư mục

```
backend/                  Spring Boot API (hexagonal)
  src/main/java/com/ragchatbot/
    domain/                model, enum, port, exception
    application/           usecase, dto
    infrastructure/        gemini, qdrant, chunking, parsing, llm, persistence, benchmark, embedding
    api/                   REST controller + Thymeleaf view (trang /documents)
    config/                Spring configuration
  src/main/resources/
    db/migration/          Flyway SQL (V1..V7)
    static/test-data/      test_set.json (50 câu hỏi benchmark)
frontend/                  React + Vite SPA (chat, documents, benchmark, settings)
embedding-service/         FastAPI: multilingual-e5-base, BGE-M3, PhoBERT
ragas-service/             FastAPI: 4 RAGAS metric qua Gemini/Ollama judge
finetune-service/          Script chuẩn bị dataset + train QLoRA
datasets/benchmark/        Test set 50 câu hỏi + ground truth
ollama-model/              Modelfile cho model fine-tune chạy qua Ollama
notes/                     Ghi chú spike kỹ thuật (parsing, chunking)
docker-compose.yml         Toàn bộ hạ tầng: postgres, qdrant, ollama, embedding-service, ragas-service, backend
```

## Bắt đầu nhanh (Docker Compose)

**Yêu cầu:** Docker Desktop, một `GEMINI_API_KEY` hợp lệ.

```bash
# 1. Tạo file .env ở thư mục gốc
echo "GEMINI_API_KEY=your_key_here" > .env

# 2. Build & chạy toàn bộ backend + hạ tầng
docker compose up -d --build

# 3. Kiểm tra health
curl http://localhost:8080/actuator/health
```

Dịch vụ sau khi lên:

| Service | Port |
|---|---|
| Backend API | `8080` |
| PostgreSQL (pgvector) | `5432` |
| Qdrant (gRPC / HTTP) | `6334` / `6333` |
| Ollama | `11434` |
| embedding-service | `8001`* |
| ragas-service | `8002` |

\* `embedding-service/Dockerfile` expose port nội bộ `8000`; `docker-compose.yml` map ra ngoài `8001:8001` — cần rà soát lại cho khớp nếu bạn build lại image.

Frontend **không** nằm trong `docker-compose.yml`, chạy riêng ở bước dưới.

## Chạy local không dùng Docker

**Backend** (cần Java 21 + Maven, Postgres/Qdrant chạy sẵn hoặc qua Docker riêng lẻ):

```bash
cd backend
mvn -B -DskipTests package
java -jar target/rag-chatbot-backend-0.0.1-SNAPSHOT.jar
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev
```

Vite dev server chạy ở `http://localhost:5173`, proxy toàn bộ `/api` sang backend `http://localhost:8080` (cấu hình trong `vite.config.js`).

**Python microservices** (tuỳ chọn, chỉ cần khi benchmark embedding model khác hoặc chạy RAGAS thật):

```bash
pip install -r requirement.txt          # root: fastapi/uvicorn dùng chung
pip install -r embedding-service/requirements.txt
pip install -r ragas-service/requirements.txt

uvicorn main:app --host 0.0.0.0 --port 8001   # trong embedding-service/
uvicorn main:app --host 0.0.0.0 --port 8002   # trong ragas-service/
```

## Biến môi trường

| Biến | Mặc định | Mô tả |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/ragchatbot` | Kết nối Postgres |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `raguser` / `ragpass` | Tài khoản DB |
| `QDRANT_HOST` / `QDRANT_PORT` | `localhost` / `6334` | Vector store |
| `QDRANT_VECTOR_SIZE` | `3072` | Phải khớp chiều embedding đang dùng (Gemini = 3072) |
| `RAG_LLM_PROVIDER` | `GEMINI` | `GEMINI` hoặc `OLLAMA` |
| `GEMINI_API_KEY` | *(bắt buộc)* | API key Gemini |
| `GEMINI_CHAT_MODEL` | `gemini-2.5-flash` | Model sinh câu trả lời |
| `GEMINI_EMBEDDING_MODEL` | `gemini-embedding-001` | Model embedding mặc định |
| `RAG_EMBEDDING_LOCAL_SERVICE_BASE_URL` | `http://localhost:8000` | Base URL của `embedding-service` (E5/PhoBERT/BGE-M3) |
| `RAGAS_SERVICE_URL` | `http://localhost:8002` | Endpoint ragas-service |
| `OLLAMA_BASE_URL` / `OLLAMA_MODEL` | `http://localhost:11434` / `hoc-phan-chatbot` | Fallback LLM local |
| `BENCHMARK_FINETUNE_OLLAMA_MODEL` | `hoc-phan-chatbot-finetuned` | Model dùng cho nhánh benchmark `FINETUNE` |

Toàn bộ mapping nằm trong `backend/src/main/resources/application.yml`.

## API chính

Swagger UI: `http://localhost:8080/swagger-ui.html`

| Method | Endpoint | Chức năng |
|---|---|---|
| `POST` | `/api/chat/message` | Gửi câu hỏi, nhận câu trả lời có trích dẫn nguồn |
| `POST` | `/api/chat/attachments` | Đính kèm file trực tiếp trong khung chat |
| `GET` | `/api/chat/attachments/{sessionId}` | Danh sách file đã đính kèm |
| `GET` | `/api/chat/history/{sessionId}` | Lịch sử hội thoại theo phiên |
| `GET` | `/api/chat/conversations` | Danh sách hội thoại gần nhất |
| `POST` | `/api/documents/upload` | Upload tài liệu môn học (chọn chunking strategy + embedding model) |
| `GET` | `/api/documents` | Danh sách tài liệu đã index |
| `GET` | `/api/documents/{id}/status` | Poll trạng thái xử lý |
| `GET` | `/api/documents/{id}/chunks` | Preview chunk (tối đa 5) |
| `DELETE` | `/api/documents/{id}` | Xóa tài liệu + dọn vector trong Qdrant |
| `POST` | `/api/evaluate` | Đánh giá 1 câu trả lời (Exact Match, F1, 4 chỉ số RAGAS) |
| `POST` | `/api/benchmark/run` | Chạy benchmark bất đồng bộ theo config (strategy, embedding, experiment type) |
| `GET` | `/api/benchmark/jobs/{jobId}/status` | Poll tiến trình benchmark |
| `GET` | `/api/benchmark/results` | Bảng tổng hợp trung bình theo nhóm (dữ liệu cho dashboard RBL) |

## Fine-tuning (QLoRA)

Thư mục `finetune-service/`:

- `prepare_dataset.py`: đọc slide (PPTX/PDF) trong `finetune-service/slides/`, dùng Gemini sinh cặp Q&A tiếng Việt, tự động loại trùng với test set (`datasets/benchmark/test_set.json`), xuất `train_dataset.json` / `train_data.jsonl`.
- `train_qlora.py`: fine-tune QLoRA (4-bit, LoRA r=16) trên base model `Qwen/Qwen2.5-1.5B-Instruct`, xuất adapter vào `uth_lora_adapter/`.
- Model sau fine-tune được đóng gói chạy qua Ollama bằng `ollama-model/Modelfile` (`hoc-phan-chatbot`), backend gọi qua `FineTunedOllamaInferenceService` khi benchmark với `experimentType=FINETUNE`.

```bash
cd finetune-service
python prepare_dataset.py
python train_qlora.py --data_path train_data.jsonl --output_dir ../uth_lora_adapter
```


---

*Đồ án học phần — Xây dựng chatbot hỏi đáp học tập và nghiên cứu so sánh RAG vs Fine-tuning cho tiếng Việt.*
