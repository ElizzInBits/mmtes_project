package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.ChatSessionRepository;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
public class ChatbotUseCase implements ChatbotService {

    private final ChatSessionRepository sessionRepository;
    private final MessagingPort messagingPort;
    private final List<StateHandler> handlers;

    public ChatbotUseCase(ChatSessionRepository sessionRepository, MessagingPort messagingPort, List<StateHandler> handlers) {
        this.sessionRepository = sessionRepository;
        this.messagingPort = messagingPort;
        this.handlers = handlers;
    }

    public void processMessage(String jid, String message, String pushName) {
        ChatSession session = sessionRepository.findByJid(jid)
                .orElseGet(() -> {
                    ChatSession newSession = new ChatSession();
                    newSession.setJid(jid);
                    newSession.setCurrentState(ChatState.IDLE);
                    newSession.setContext(new HashMap<>());
                    newSession.setLastInteraction(LocalDateTime.now());
                    sessionRepository.save(newSession);
                    return newSession;
                });

        if (!session.getContext().containsKey("pushName")) {
            session.getContext().put("pushName", pushName);
        }

        if (message.equalsIgnoreCase("sair") || message.equalsIgnoreCase("cancelar")) {
            messagingPort.sendMessage(jid, "Atendimento encerrado.");
            sessionRepository.deleteByJid(jid);
            return;
        }

        if (message.equalsIgnoreCase("voltar")) {
            session.goBack();
            handlers.stream()
                .filter(h -> h.getSupportedState().equals(session.getCurrentState()))
                .findFirst()
                .ifPresent(handler -> handler.handle(session, "RESHOW"));
            sessionRepository.save(session);
            return;
        }

        handlers.stream()
                .filter(h -> h.getSupportedState().equals(session.getCurrentState()))
                .findFirst()
                .ifPresentOrElse(
                        handler -> handler.handle(session, message),
                        () -> handleUnknownState(session)
                );

        sessionRepository.save(session);
    }

    private void handleUnknownState(ChatSession session) {
        messagingPort.sendMessage(session.getJid(), "Olá! Digite 'Oi' para iniciar.");
        session.updateState(ChatState.IDLE);
    }
}
