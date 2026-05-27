package com.horacerta.backend.domain.port.out;

import com.horacerta.backend.domain.model.ChatSession;

import java.util.Optional;

public interface ChatSessionRepository {
    Optional<ChatSession> findByJid(String jid);
    void save(ChatSession session);
    void deleteByJid(String jid);
}
