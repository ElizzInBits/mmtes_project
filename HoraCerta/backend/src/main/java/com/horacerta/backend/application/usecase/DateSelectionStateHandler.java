package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.CalendarPort;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DateSelectionStateHandler implements StateHandler {

    private final MessagingPort messagingPort;
    private final CalendarPort calendarPort;

    public DateSelectionStateHandler(MessagingPort messagingPort, CalendarPort calendarPort) {
        this.messagingPort = messagingPort;
        this.calendarPort = calendarPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.ESCOLHENDO_DATA;
    }

    @Override
    public void handle(ChatSession session, String message) {
        if (message.equals("RESHOW")) return;
        List<LocalDateTime> slots = calendarPort.getAvailableSlots(LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        if (slots.isEmpty()) {
            messagingPort.sendMessage(session.getJid(), "Sem horários.");
            return;
        }
        StringBuilder response = new StringBuilder("Escolha:\n");
        for (int i = 0; i < slots.size(); i++) response.append(i + 1).append(". ").append(slots.get(i)).append("\n");
        session.getContext().put("availableSlots", slots);
        messagingPort.sendMessage(session.getJid(), response.toString());
        session.updateState(ChatState.ESCOLHENDO_HORARIO);
    }
}
