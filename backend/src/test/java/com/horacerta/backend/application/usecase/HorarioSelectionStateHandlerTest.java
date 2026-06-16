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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class HorarioSelectionStateHandlerTest {

    private HorarioSelectionStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @Mock
    private CalendarPort calendarPort;

    private final LocalDateTime slot1 = LocalDateTime.of(2026, 6, 15, 9, 0);
    private final LocalDateTime slot2 = LocalDateTime.of(2026, 6, 15, 10, 0);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new HorarioSelectionStateHandler(messagingPort, calendarPort);
    }

    @Test
    void shouldSupportEscolhendoHorarioState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.ESCOLHENDO_HORARIO);
    }

    @Test
    void shouldSelectValidSlotAndTransitionToColetandoNome() {
        ChatSession session = sessionWithSlots();

        handler.handle(session, "1");

        assertThat(session.getContext().get("selectedTime")).isEqualTo(slot1.toString());
        assertThat(session.getCurrentState()).isEqualTo(ChatState.COLETANDO_NOME);
        verify(messagingPort).sendMessage(eq("jid1"), contains("NOME COMPLETO"));
    }

    @Test
    void shouldSelectSecondSlotCorrectly() {
        ChatSession session = sessionWithSlots();

        handler.handle(session, "2");

        assertThat(session.getContext().get("selectedTime")).isEqualTo(slot2.toString());
        assertThat(session.getCurrentState()).isEqualTo(ChatState.COLETANDO_NOME);
    }

    @Test
    void shouldSendErrorOnOutOfRangeSelection() {
        ChatSession session = sessionWithSlots();

        handler.handle(session, "99");

        verify(messagingPort).sendMessage(eq("jid1"), contains("inválida"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.ESCOLHENDO_HORARIO);
    }

    @Test
    void shouldSendErrorOnZeroSelection() {
        ChatSession session = sessionWithSlots();

        handler.handle(session, "0");

        verify(messagingPort).sendMessage(eq("jid1"), contains("inválida"));
    }

    @Test
    void shouldSendErrorOnNonNumericInput() {
        ChatSession session = sessionWithSlots();

        handler.handle(session, "segunda");

        verify(messagingPort).sendMessage(eq("jid1"), contains("número"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.ESCOLHENDO_HORARIO);
    }

    @Test
    void shouldIgnoreReshowMessage() {
        ChatSession session = sessionWithSlots();

        handler.handle(session, "RESHOW");

        verifyNoInteractions(messagingPort);
    }

    // --- helpers ---

    private ChatSession sessionWithSlots() {
        ChatSession session = new ChatSession();
        session.setJid("jid1");
        session.setCurrentState(ChatState.ESCOLHENDO_HORARIO);
        session.setContext(new HashMap<>());
        session.getContext().put("availableSlots", List.of(slot1, slot2));
        return session;
    }
}
