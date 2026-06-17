package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.CalendarPort;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class HorarioSelectionStateHandler implements StateHandler {

    private final MessagingPort messagingPort;
    private final CalendarPort calendarPort;

    public HorarioSelectionStateHandler(MessagingPort messagingPort, CalendarPort calendarPort) {
        this.messagingPort = messagingPort;
        this.calendarPort = calendarPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.ESCOLHENDO_HORARIO;
    }

    @Override
    public void handle(ChatSession session, String message) {
        if (message.equals("RESHOW")) return;

        List<LocalDateTime> availableSlots = (List<LocalDateTime>) session.getContext().get("availableSlots");
        
        try {
            int selection = Integer.parseInt(message) - 1;
            if (selection < 0 || selection >= availableSlots.size()) {
                messagingPort.sendMessage(session.getJid(), "Opção inválida. Escolha um número da lista.");
                return;
            }

            LocalDateTime selectedTime = availableSlots.get(selection);
            session.getContext().put("selectedTime", selectedTime.toString());
            
            messagingPort.sendMessage(session.getJid(), "Horário selecionado: " + selectedTime.toString() + 
                    "\nAgora, por favor, digite seu NOME COMPLETO:");
            
            session.updateState(ChatState.COLETANDO_NOME);
            
        } catch (NumberFormatException e) {
            messagingPort.sendMessage(session.getJid(), "Por favor, digite apenas o número do horário desejado.");
        }
    }
}
