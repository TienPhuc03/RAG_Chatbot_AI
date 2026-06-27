import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import ChatComposer from "../components/ChatComposer";
import ChatMessage from "../components/ChatMessage";
import ChatWelcome from "../components/ChatWelcome";
import { useConversations } from "../context/ConversationsContext";
import { fetchChatHistory, sendMessage } from "../services/chatService";

function toUiMessage(message) {
  return {
    id: message.messageId || `${message.role}-${message.createdAt}`,
    role: message.role,
    content: message.content,
    createdAt: message.createdAt,
    groundedInDocuments: false,
  };
}

function ChatPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const { refreshConversations } = useConversations();
  const [composerValue, setComposerValue] = useState("");
  const [messages, setMessages] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [sendLoading, setSendLoading] = useState(false);
  const [error, setError] = useState("");
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, historyLoading]);

  useEffect(() => {
    if (!sessionId) {
      setMessages([]);
      setError("");
      return;
    }

    let active = true;

    async function loadHistory() {
      setHistoryLoading(true);
      setError("");
      setMessages([]);

      try {
        const response = await fetchChatHistory(sessionId);
        if (!active) {
          return;
        }
        setMessages(response.map(toUiMessage));
      } catch (loadError) {
        if (!active) {
          return;
        }
        setError(loadError.message || "Unable to load this conversation.");
      } finally {
        if (active) {
          setHistoryLoading(false);
        }
      }
    }

    loadHistory();

    return () => {
      active = false;
    };
  }, [sessionId]);

  const isWelcomeState = !sessionId && !historyLoading && messages.length === 0 && !error;

  const handleSubmit = async (prompt = composerValue) => {
    const trimmedPrompt = prompt.trim();

    if (!trimmedPrompt || sendLoading) {
      return;
    }

    const optimisticMessage = {
      id: `local-user-${Date.now()}`,
      role: "USER",
      content: trimmedPrompt,
      createdAt: new Date().toISOString(),
      groundedInDocuments: false,
    };

    setComposerValue("");
    setError("");
    setMessages((current) => [...current, optimisticMessage]);
    setSendLoading(true);

    try {
      const response = await sendMessage({
        sessionId: sessionId || null,
        question: trimmedPrompt,
        courseCode: "",
        chapterCode: "",
      });

      const assistantMessage = {
        id: `assistant-${Date.now()}`,
        role: "ASSISTANT",
        content: response.answer,
        createdAt: new Date().toISOString(),
        groundedInDocuments: response.groundedInDocuments,
      };

      setMessages((current) => [...current, assistantMessage]);
      await refreshConversations();

      if (!sessionId && response.sessionId) {
        navigate(`/chat/${response.sessionId}`, { replace: true });
      }
    } catch (sendError) {
      setMessages((current) => current.filter((message) => message.id !== optimisticMessage.id));
      setError(sendError.message || "Unable to send your message.");
    } finally {
      setSendLoading(false);
    }
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="scrollbar-subtle flex-1 overflow-y-auto">
        {isWelcomeState ? (
          <ChatWelcome
            onPromptSelect={(prompt) => {
              setComposerValue(prompt);
            }}
          />
        ) : (
          <div className="mx-auto flex w-full max-w-4xl flex-col gap-6 px-4 py-8 sm:px-6 lg:px-8">
            {historyLoading ? (
              <div className="rounded-[1.75rem] border border-border-subtle bg-white px-6 py-5 text-text-secondary shadow-soft">
                Loading chat history...
              </div>
            ) : null}

            {messages.map((message) => (
              <ChatMessage key={message.id} message={message} />
            ))}

            {error ? (
              <ChatMessage
                message={{
                  id: "error-state",
                  role: "ERROR",
                  content: error,
                  createdAt: new Date().toISOString(),
                }}
              />
            ) : null}
            <div ref={endRef} />
          </div>
        )}
      </div>

      <div className="px-4 text-center text-sm text-text-muted sm:px-6 lg:px-8">
        <p className="pb-2">AI can make mistakes. Verify important information.</p>
      </div>

      <ChatComposer
        value={composerValue}
        onChange={setComposerValue}
        onSubmit={() => handleSubmit()}
        loading={sendLoading}
      />
    </div>
  );
}

export default ChatPage;
