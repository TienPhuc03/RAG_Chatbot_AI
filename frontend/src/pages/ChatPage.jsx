import {
  useEffect,
  useRef,
  useState,
} from "react";
import {
  useNavigate,
  useParams,
} from "react-router-dom";

import ChatComposer from "../components/ChatComposer";
import ChatMessage from "../components/ChatMessage";
import ChatWelcome from "../components/ChatWelcome";
import { useConversations } from "../context/ConversationsContext";
import { fetchDocumentStatus } from "../services/documentService";
import {
  fetchChatAttachments,
  fetchChatHistory,
  sendMessage,
  uploadChatAttachment,
} from "../services/chatService";

const DEFAULT_EMBEDDING_MODEL = "BGE_M3";
const EMBEDDING_MODEL_STORAGE_KEY =
  "chatEmbeddingModel";

const EMBEDDING_MODEL_OPTIONS = [
  {
    value: "BGE_M3",
    label: "BGE_M3",
  },
  {
    value: "MULTILINGUAL_E5_BASE",
    label: "MULTILINGUAL_E5_BASE",
  },
  {
    value: "PHOBERT_BASE",
    label: "PHOBERT_BASE",
  },
  {
    value: "GEMINI_EMBEDDING_001",
    label: "GEMINI_EMBEDDING_001",
  },
];

function normalizeCitations(citations) {
  return Array.isArray(citations)
    ? citations
    : [];
}

function toUiMessage(message) {
  const citations =
    normalizeCitations(
      message.citations
    );

  return {
    id:
      message.messageId
      || `${message.role}-${message.createdAt}`,
    role: message.role,
    content: message.content,
    createdAt: message.createdAt,
    groundedInDocuments: Boolean(
      message.groundedInDocuments
      || citations.length > 0
    ),
    citations,
  };
}

function wait(milliseconds) {
  return new Promise((resolve) => {
    setTimeout(
      resolve,
      milliseconds
    );
  });
}

function readStoredEmbeddingModel() {
  const storedModel =
    localStorage.getItem(
      EMBEDDING_MODEL_STORAGE_KEY
    );

  const isSupported =
    EMBEDDING_MODEL_OPTIONS.some(
      (option) =>
        option.value === storedModel
    );

  return isSupported
    ? storedModel
    : DEFAULT_EMBEDDING_MODEL;
}

function ChatPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();

  const {
    refreshConversations,
  } = useConversations();

  const [
    composerValue,
    setComposerValue,
  ] = useState("");

  const [
    courseCode,
    setCourseCode,
  ] = useState("");

  const [
    chapterCode,
    setChapterCode,
  ] = useState("");

  const [
    embeddingModel,
    setEmbeddingModel,
  ] = useState(
    readStoredEmbeddingModel
  );

  const [
    messages,
    setMessages,
  ] = useState([]);

  const [
    attachments,
    setAttachments,
  ] = useState([]);

  const [
    selectedFile,
    setSelectedFile,
  ] = useState(null);

  const [
    attachmentNotice,
    setAttachmentNotice,
  ] = useState("");

  const [
    historyLoading,
    setHistoryLoading,
  ] = useState(false);

  const [
    sendLoading,
    setSendLoading,
  ] = useState(false);

  const [
    error,
    setError,
  ] = useState("");

  const endRef = useRef(null);

  useEffect(() => {
    localStorage.setItem(
      EMBEDDING_MODEL_STORAGE_KEY,
      embeddingModel
    );
  }, [embeddingModel]);

  useEffect(() => {
    endRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "end",
    });
  }, [
    messages,
    historyLoading,
  ]);

  useEffect(() => {
    if (!sessionId) {
      setMessages([]);
      setAttachments([]);
      setAttachmentNotice("");
      setError("");
      return;
    }

    let active = true;

    async function loadConversationData() {
      setHistoryLoading(true);
      setError("");
      setMessages([]);

      try {
        const [
          historyResponse,
          attachmentResponse,
        ] = await Promise.all([
          fetchChatHistory(sessionId),
          fetchChatAttachments(sessionId),
        ]);

        if (!active) {
          return;
        }

        setMessages(
          historyResponse.map(
            toUiMessage
          )
        );

        setAttachments(
          Array.isArray(
            attachmentResponse
          )
            ? attachmentResponse
            : []
        );
      } catch (loadError) {
        if (active) {
          setError(
            loadError.message
            || "Không tải được cuộc trò chuyện này."
          );
        }
      } finally {
        if (active) {
          setHistoryLoading(false);
        }
      }
    }

    loadConversationData();

    return () => {
      active = false;
    };
  }, [sessionId]);

  const isWelcomeState =
    !sessionId
    && !historyLoading
    && messages.length === 0
    && !error;

  const updateAttachmentItem = (
    documentId,
    patch
  ) => {
    setAttachments((current) =>
      current.map((attachment) =>
        attachment.documentId
          === documentId
          ? {
              ...attachment,
              ...patch,
            }
          : attachment
      )
    );
  };

  const handleSubmit = async (
    prompt = composerValue
  ) => {
    const trimmedPrompt =
      prompt.trim();

    if (
      !trimmedPrompt
      || sendLoading
    ) {
      return;
    }

    setError("");
    setSendLoading(true);

    try {
      let activeSessionId =
        sessionId || null;

      if (selectedFile) {
        setAttachmentNotice(
          `Đang tải tệp ${selectedFile.name} bằng ${embeddingModel}...`
        );

        const uploadResponse =
          await uploadChatAttachment({
            sessionId:
              activeSessionId,
            file: selectedFile,
            embeddingModel,
          });

        activeSessionId =
          uploadResponse.sessionId;

        const uploadedAttachment = {
          documentId:
            uploadResponse.documentId,
          fileName:
            uploadResponse.fileName,
          status:
            uploadResponse.status,
          failureReason:
            uploadResponse.failureReason,
          indexedAt: null,
          embeddingModel,
        };

        setAttachments(
          (current) => [
            ...current,
            uploadedAttachment,
          ]
        );

        setSelectedFile(null);

        if (
          !sessionId
          && activeSessionId
        ) {
          navigate(
            `/chat/${activeSessionId}`,
            {
              replace: true,
            }
          );
        }

        let currentStatus =
          uploadResponse.status;

        let failureReason =
          uploadResponse.failureReason
          || "";

        setAttachmentNotice(
          `Đang đọc và index tệp bằng ${embeddingModel}...`
        );

        while (
          currentStatus
            === "PENDING"
          || currentStatus
            === "PROCESSING"
        ) {
          await wait(2000);

          const statusResponse =
            await fetchDocumentStatus(
              uploadResponse.documentId
            );

          currentStatus =
            statusResponse.status;

          failureReason =
            statusResponse.failureReason
            || "";

          updateAttachmentItem(
            uploadResponse.documentId,
            {
              status:
                currentStatus,
              failureReason,
              indexedAt:
                statusResponse.indexedAt,
              embeddingModel,
            }
          );
        }

        if (
          currentStatus === "FAILED"
        ) {
          setAttachmentNotice(
            failureReason
            || "Tệp xử lý thất bại."
          );

          await refreshConversations();
          return;
        }

        setAttachmentNotice(
          `Tệp ${uploadResponse.fileName} đã sẵn sàng với ${embeddingModel}.`
        );
      }

      const optimisticMessage = {
        id:
          `local-user-${Date.now()}`,
        role: "USER",
        content: trimmedPrompt,
        createdAt:
          new Date().toISOString(),
        groundedInDocuments: false,
        citations: [],
      };

      setComposerValue("");

      setMessages((current) => [
        ...current,
        optimisticMessage,
      ]);

      const response =
        await sendMessage({
          sessionId:
            activeSessionId,
          question:
            trimmedPrompt,
          courseCode:
            courseCode.trim(),
          chapterCode:
            chapterCode.trim(),
          embeddingModel,
        });

      const assistantMessage = {
        id:
          `assistant-${Date.now()}`,
        role: "ASSISTANT",
        content: response.answer,
        createdAt:
          new Date().toISOString(),
        groundedInDocuments:
          Boolean(
            response.groundedInDocuments
            || normalizeCitations(
              response.citations
            ).length > 0
          ),
        citations:
          normalizeCitations(
            response.citations
          ),
      };

      setMessages((current) => [
        ...current,
        assistantMessage,
      ]);

      await refreshConversations();

      if (
        !sessionId
        && response.sessionId
      ) {
        navigate(
          `/chat/${response.sessionId}`,
          {
            replace: true,
          }
        );
      }
    } catch (sendError) {
      setError(
        sendError.message
        || "Không gửi được tin nhắn của bạn."
      );

      await refreshConversations();
    } finally {
      setSendLoading(false);
    }
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="mx-auto w-full max-w-5xl px-4 pt-6 sm:px-6 lg:px-8">
        <div className="grid gap-4 rounded-[1.75rem] border border-border-subtle bg-white p-5 shadow-soft md:grid-cols-3">
          <label className="text-sm text-text-secondary">
            Mã học phần

            <input
              value={courseCode}
              onChange={(event) =>
                setCourseCode(
                  event.target.value
                )
              }
              placeholder="VD: DSA"
              disabled={sendLoading}
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4 text-text-primary outline-none disabled:bg-slate-100"
            />
          </label>

          <label className="text-sm text-text-secondary">
            Mã chương

            <input
              value={chapterCode}
              onChange={(event) =>
                setChapterCode(
                  event.target.value
                )
              }
              placeholder="VD: DSA02"
              disabled={sendLoading}
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4 text-text-primary outline-none disabled:bg-slate-100"
            />
          </label>

          <label className="text-sm text-text-secondary">
            Embedding model

            <select
              value={embeddingModel}
              onChange={(event) =>
                setEmbeddingModel(
                  event.target.value
                )
              }
              disabled={sendLoading}
              className="mt-2 h-11 w-full rounded-2xl border border-border-subtle px-4 text-text-primary outline-none disabled:bg-slate-100"
            >
              {EMBEDDING_MODEL_OPTIONS.map(
                (option) => (
                  <option
                    key={option.value}
                    value={option.value}
                  >
                    {option.label}
                  </option>
                )
              )}
            </select>
          </label>

          <div className="md:col-span-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs leading-5 text-amber-900">
            Model được chọn phải trùng với model đã dùng để index tài liệu. Khi upload tệp đính kèm, hệ thống sẽ dùng chính model đang chọn để index và truy xuất.
          </div>
        </div>
      </div>

      <div className="scrollbar-subtle flex-1 overflow-y-auto">
        {isWelcomeState ? (
          <ChatWelcome
            onPromptSelect={(prompt) => {
              setComposerValue(
                prompt
              );
            }}
          />
        ) : (
          <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 py-8 sm:px-6 lg:px-8">
            {historyLoading ? (
              <div className="rounded-[1.75rem] border border-border-subtle bg-white px-6 py-5 text-text-secondary shadow-soft">
                Đang tải lịch sử trò chuyện...
              </div>
            ) : null}

            {messages.map(
              (message) => (
                <ChatMessage
                  key={message.id}
                  message={message}
                />
              )
            )}

            {error ? (
              <ChatMessage
                message={{
                  id: "error-state",
                  role: "ERROR",
                  content: error,
                  createdAt:
                    new Date().toISOString(),
                  citations: [],
                }}
              />
            ) : null}

            <div ref={endRef} />
          </div>
        )}
      </div>

      <div className="px-4 text-center text-sm text-text-muted sm:px-6 lg:px-8">
        <p className="pb-2">
          AI có thể mắc lỗi. Hãy xác minh các thông tin quan trọng.
        </p>
      </div>

      <ChatComposer
        value={composerValue}
        onChange={setComposerValue}
        onSubmit={() =>
          handleSubmit()
        }
        loading={sendLoading}
        selectedFile={selectedFile}
        attachments={attachments}
        attachmentNotice={
          attachmentNotice
        }
        onPickFile={
          setSelectedFile
        }
        onRemoveSelectedFile={() =>
          setSelectedFile(null)
        }
      />
    </div>
  );
}

export default ChatPage;