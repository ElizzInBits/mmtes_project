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

class MenuPrincipalStateHandlerTest {

    private MenuPrincipalStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new MenuPrincipalStateHandler(messagingPort);
    }

    @Test
    void shouldSupportMenuPrincipalState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.MENU_PRINCIPAL);
    }

    @Test
    void shouldSendCatalogLinkOnOption1() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "1");

        verify(messagingPort).sendMessage(eq("jid1"), contains("Catálogo"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.IDLE);
    }

    @Test
    void shouldTransitionToOrcamentoOnOption2() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "2");

        verify(messagingPort).sendMessage(eq("jid1"), contains("Descreva"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.SOLICITANDO_ORCAMENTO);
    }

    @Test
    void shouldTransitionToEscolhendoDataOnOption3() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "3");

        assertThat(session.getCurrentState()).isEqualTo(ChatState.ESCOLHENDO_DATA);
    }

    @Test
    void shouldSendErrorMessageOnInvalidOption() {
        ChatSession session = sessionWith("jid1");
        ChatState stateBefore = session.getCurrentState();

        handler.handle(session, "9");

        verify(messagingPort).sendMessage(eq("jid1"), contains("1, 2 ou 3"));
        assertThat(session.getCurrentState()).isEqualTo(stateBefore);
    }

    @Test
    void shouldSendErrorMessageOnTextInput() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "agendar");

        verify(messagingPort).sendMessage(eq("jid1"), contains("1, 2 ou 3"));
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(ChatState.MENU_PRINCIPAL);
        session.setContext(new HashMap<>());
        return session;
    }
}
