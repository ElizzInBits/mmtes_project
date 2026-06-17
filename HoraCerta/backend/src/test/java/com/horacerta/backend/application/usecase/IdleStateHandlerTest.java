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

class IdleStateHandlerTest {

    private IdleStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new IdleStateHandler(messagingPort);
    }

    @Test
    void shouldSupportIdleState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.IDLE);
    }

    @Test
    void shouldSendWelcomeMessageWithPushName() {
        ChatSession session = sessionWith("55199@s.whatsapp.net", ChatState.IDLE);
        session.getContext().put("pushName", "Maria");

        handler.handle(session, "oi");

        verify(messagingPort).sendMessage(eq("55199@s.whatsapp.net"), contains("Maria"));
    }

    @Test
    void shouldSendWelcomeMessageWithDefaultNameWhenPushNameAbsent() {
        ChatSession session = sessionWith("55199@s.whatsapp.net", ChatState.IDLE);

        handler.handle(session, "oi");

        verify(messagingPort).sendMessage(eq("55199@s.whatsapp.net"), contains("cliente"));
    }

    @Test
    void shouldTransitionToMenuPrincipal() {
        ChatSession session = sessionWith("55199@s.whatsapp.net", ChatState.IDLE);

        handler.handle(session, "oi");

        assertThat(session.getCurrentState()).isEqualTo(ChatState.MENU_PRINCIPAL);
    }

    @Test
    void shouldIncludeMenuOptionsInMessage() {
        ChatSession session = sessionWith("55199@s.whatsapp.net", ChatState.IDLE);

        handler.handle(session, "oi");

        verify(messagingPort).sendMessage(
            eq("55199@s.whatsapp.net"),
            contains("1")
        );
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid, ChatState state) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(state);
        session.setContext(new HashMap<>());
        return session;
    }
}
