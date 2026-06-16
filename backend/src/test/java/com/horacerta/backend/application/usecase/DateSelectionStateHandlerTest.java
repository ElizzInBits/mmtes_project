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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DateSelectionStateHandlerTest {

    private DateSelectionStateHandler handler;

    @Mock
    private MessagingPort messagingPort;

    @Mock
    private CalendarPort calendarPort;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new DateSelectionStateHandler(messagingPort, calendarPort);
    }

    @Test
    void shouldSupportEscolhendoDataState() {
        assertThat(handler.getSupportedState()).isEqualTo(ChatState.ESCOLHENDO_DATA);
    }

    @Test
    void shouldShowAvailableSlotsAndTransitionToEscolhendoHorario() {
        ChatSession session = sessionWith("jid1");
        List<LocalDateTime> slots = List.of(
            LocalDateTime.of(2026, 6, 15, 9, 0),
            LocalDateTime.of(2026, 6, 15, 10, 0)
        );
        when(calendarPort.getAvailableSlots(any(), any())).thenReturn(slots);

        handler.handle(session, "qualquer");

        verify(messagingPort).sendMessage(eq("jid1"), contains("1."));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.ESCOLHENDO_HORARIO);
        assertThat(session.getContext().get("availableSlots")).isEqualTo(slots);
    }

    @Test
    void shouldSendNoSlotsMessageWhenNoneAvailable() {
        ChatSession session = sessionWith("jid1");
        when(calendarPort.getAvailableSlots(any(), any())).thenReturn(Collections.emptyList());

        handler.handle(session, "qualquer");

        verify(messagingPort).sendMessage(eq("jid1"), contains("Sem horários"));
        assertThat(session.getCurrentState()).isEqualTo(ChatState.ESCOLHENDO_DATA);
    }

    @Test
    void shouldIgnoreReshowMessage() {
        ChatSession session = sessionWith("jid1");

        handler.handle(session, "RESHOW");

        verifyNoInteractions(messagingPort);
        verifyNoInteractions(calendarPort);
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(ChatState.ESCOLHENDO_DATA);
        session.setContext(new HashMap<>());
        return session;
    }
}
