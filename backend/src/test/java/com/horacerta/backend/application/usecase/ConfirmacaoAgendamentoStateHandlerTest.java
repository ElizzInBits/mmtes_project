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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class ConfirmacaoAgendamentoStateHandlerTest {

    private ConfirmacaoAgendamentoStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @Mock
    private CalendarPort calendarPort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new ConfirmacaoAgendamentoStateHandler(messagingPort, calendarPort);
    }

    @Test
    void shouldSupportConfirmandoAgendamentoState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.CONFIRMANDO_AGENDAMENTO);
    }

    @Test
    void shouldCreateCalendarEventAndConfirmOnSim() throws Exception {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "SIM");

        verify(calendarPort).scheduleEvent(
            contains("João Silva"),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any()
        );
        verify(messagingPort).sendMessage(eq("jid1"), contains("sucesso"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.AGENDAMENTO_FINALIZADO);
    }

    @Test
    void shouldConfirmCaseInsensitive() throws Exception {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "sim");

        verify(calendarPort).scheduleEvent(any(), any(), any(), any());
    }

    @Test
    void shouldSendErrorMessageWhenCalendarFails() throws Exception {
        ChatSession session = sessionWith("jid1");
        doThrow(new RuntimeException("Calendar indisponível"))
            .when(calendarPort).scheduleEvent(any(), any(), any(), any());

        handler.handle(session, "SIM");

        verify(messagingPort).sendMessage(eq("jid1"), contains("Erro"));
    }

    @Test
    void shouldAskForSimWhenResponseIsNotSim() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "talvez");

        verify(messagingPort).sendMessage(eq("jid1"), contains("SIM"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.CONFIRMANDO_AGENDAMENTO);
    }

    @Test
    void shouldScheduleEventEndingOneHourAfterStart() throws Exception {
        ChatSession session = sessionWith("jid1");
        LocalDateTime start = LocalDateTime.of(2026, 6, 15, 9, 0);

        handler.handle(session, "SIM");

        verify(calendarPort).scheduleEvent(
            any(),
            eq(start),
            eq(start.plusHours(1)),
            any()
        );
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(ChatState.CONFIRMANDO_AGENDAMENTO);
        session.setContext(new HashMap<>());
        session.getContext().put("nomeCompleto", "João Silva");
        session.getContext().put("cpf", "12345678901");
        session.getContext().put("selectedTime", LocalDateTime.of(2026, 6, 15, 9, 0).toString());
        return session;
    }
}
