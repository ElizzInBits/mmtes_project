package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.CalendarPort;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CpfColetaStateHandlerTest {

    private CpfColetaStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @Mock
    private CalendarPort calendarPort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CpfColetaStateHandler(messagingPort, calendarPort);
    }

    @Test
    void shouldSupportColetandoCpfState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.COLETANDO_CPF);
    }

    @Test
    void shouldAcceptValidCpfAndShowConfirmation() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "12345678901");

        assertThat(session.getContext().get("cpf")).isEqualTo("12345678901");
        assertThat(session.getCurrentState()).isEqualTo(ChatState.CONFIRMANDO_AGENDAMENTO);
        verify(messagingPort).sendMessage(eq("jid1"), contains("Confirmando"));
    }

    @Test
    void shouldAcceptCpfWithMask() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "123.456.789-01");

        assertThat(session.getContext().get("cpf")).isEqualTo("123.456.789-01");
        assertThat(session.getCurrentState()).isEqualTo(ChatState.CONFIRMANDO_AGENDAMENTO);
    }

    @Test
    void shouldRejectCpfWithLessThan11Digits() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "1234567");

        verify(messagingPort).sendMessage(eq("jid1"), contains("inválido"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.COLETANDO_CPF);
    }

    @Test
    void shouldRejectCpfWithMoreThan11Digits() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "123456789012");

        verify(messagingPort).sendMessage(eq("jid1"), contains("inválido"));
    }

    @Test
    void shouldShowNameAndDateInConfirmationSummary() {
        ChatSession session = sessionWith("jid1");
        session.getContext().put("nomeCompleto", "Ana Paula");

        handler.handle(session, "12345678901");

        verify(messagingPort).sendMessage(eq("jid1"), contains("Ana Paula"));
    }

    @Test
    void shouldIgnoreReshowMessage() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "RESHOW");

        verifyNoInteractions(messagingPort);
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(ChatState.COLETANDO_CPF);
        session.setContext(new HashMap<>());
        session.getContext().put("nomeCompleto", "Cliente Teste");
        session.getContext().put("selectedTime", LocalDateTime.of(2026, 6, 15, 9, 0).toString());
        return session;
    }
}
