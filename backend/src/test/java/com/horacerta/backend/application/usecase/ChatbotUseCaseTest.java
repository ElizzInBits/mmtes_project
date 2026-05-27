package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.ChatSessionRepository;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatbotUseCaseTest {

    private ChatbotUseCase chatbotUseCase;

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private MessagingPort messagingPort;

    @Mock
    private StateHandler idleHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        List<StateHandler> handlers = new ArrayList<>();
        handlers.add(idleHandler);
        
        when(idleHandler.getSupportedState()).thenReturn(ChatState.IDLE);
        
        chatbotUseCase = new ChatbotUseCase(sessionRepository, messagingPort, handlers);
    }

    @Test
    void shouldCreateNewSessionWhenNoneExists() {
        String jid = "123456@s.whatsapp.net";
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.empty());

        chatbotUseCase.processMessage(jid, "Oi", "User");

        verify(sessionRepository).save(any(ChatSession.class));
        verify(idleHandler).handle(any(ChatSession.class), eq("Oi"));
    }

    @Test
    void shouldResetSessionOnSair() {
        String jid = "123456@s.whatsapp.net";
        ChatSession session = new ChatSession();
        session.setJid(jid);
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(session));

        chatbotUseCase.processMessage(jid, "sair", "User");

        verify(messagingPort).sendMessage(eq(jid), contains("Atendimento encerrado"));
        verify(sessionRepository).deleteByJid(jid);
    }
}
