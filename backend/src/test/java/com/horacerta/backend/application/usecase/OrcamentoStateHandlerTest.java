package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OrcamentoStateHandlerTest {

    private OrcamentoStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new OrcamentoStateHandler(messagingPort);
    }

    @Test
    void shouldSupportSolicitandoOrcamentoState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.SOLICITANDO_ORCAMENTO);
    }

    @Test
    void shouldConfirmOrcamentoAndReturnToIdle() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "Quero pintar a fachada");

        verify(messagingPort).sendMessage(eq("jid1"), contains("Quero pintar a fachada"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.IDLE);
    }

    @Test
    void shouldSendConfirmationMessage() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "Instalação elétrica");

        verify(messagingPort).sendMessage(eq("jid1"), contains("orçamento"));
    }

    @Test
    void shouldIgnoreReshowMessage() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "RESHOW");

        verifyNoInteractions(messagingPort);
        assertThat(session.getCurrentState()).isEqualTo(ChatState.SOLICITANDO_ORCAMENTO);
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(ChatState.SOLICITANDO_ORCAMENTO);
        session.setContext(new HashMap<>());
        return session;
    }
}
