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

class NomeColetaStateHandlerTest {

    private NomeColetaStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new NomeColetaStateHandler(messagingPort);
    }

    @Test
    void shouldSupportColetandoNomeState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.COLETANDO_NOME);
    }

    @Test
    void shouldStoreNameAndTransitionToColetandoCpf() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "João da Silva");

        assertThat(session.getContext().get("nomeCompleto")).isEqualTo("João da Silva");
        assertThat(session.getCurrentState()).isEqualTo(ChatState.COLETANDO_CPF);
    }

    @Test
    void shouldRequestCpfAfterName() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "Maria Souza");

        verify(messagingPort).sendMessage(eq("jid1"), contains("CPF"));
    }

    @Test
    void shouldIgnoreReshowMessage() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "RESHOW");

        verifyNoInteractions(messagingPort);
        assertThat(session.getContext().containsKey("nomeCompleto")).isFalse();
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(ChatState.COLETANDO_NOME);
        session.setContext(new HashMap<>());
        return session;
    }
}
