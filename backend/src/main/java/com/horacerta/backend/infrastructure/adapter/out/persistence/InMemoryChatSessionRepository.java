package com.horacerta.backend.infrastructure.adapter.out.persistence;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.port.out.ChatSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryChatSessionRepository implements ChatSessionRepository {

    private final Map<String, ChatSession> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<ChatSession> findByJid(String jid) {
        return Optional.ofNullable(storage.get(jid));
    }

    @Override
    public void save(ChatSession session) {
        storage.put(session.getJid(), session);
    }

    @Override
    public void deleteByJid(String jid) {
        storage.remove(jid);
    }
}
