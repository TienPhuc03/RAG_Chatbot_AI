import { createContext, useContext, useEffect, useState } from "react";
import { isAuthenticated } from "../lib/auth";
import { fetchConversations } from "../services/chatService";

const ConversationsContext = createContext(null);

export function ConversationsProvider({ children }) {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(false);

  async function refreshConversations() {
    if (!isAuthenticated()) {
      setConversations([]);
      return;
    }

    setLoading(true);

    try {
      const data = await fetchConversations();
      setConversations(data);
    } catch (error) {
      setConversations([]);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refreshConversations();
  }, []);

  return (
    <ConversationsContext.Provider
      value={{
        conversations,
        loading,
        refreshConversations,
      }}
    >
      {children}
    </ConversationsContext.Provider>
  );
}

export function useConversations() {
  const context = useContext(ConversationsContext);

  if (!context) {
    throw new Error("useConversations must be used within ConversationsProvider");
  }

  return context;
}
