package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.CalendarPort;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ConfirmacaoAgendamentoStateHandler implements StateHandler {

    private final MessagingPort messagingPort;
    private final CalendarPort calendarPort;

    public ConfirmacaoAgendamentoStateHandler(MessagingPort messagingPort, CalendarPort calendarPort) {
        this.messagingPort = messagingPort;
        this.calendarPort = calendarPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.CONFIRMANDO_AGENDAMENTO;
    }

    @Override
    public void handle(ChatSession session, String message) {
        if (message.equalsIgnoreCase("SIM")) {
            String nome = (String) session.getContext().get("nomeCompleto");
            String cpf = (String) session.getContext().get("cpf");
            LocalDateTime start = LocalDateTime.parse((String) session.getContext().get("selectedTime"));
            LocalDateTime end = start.plusHours(1);

            try {
                calendarPort.scheduleEvent(
                        "Agendamento: " + nome + " (CPF: " + cpf + ")",
                        start,
                        end,
                        null
                );

                messagingPort.sendMessage(session.getJid(), "Agendamento realizado com sucesso!");
                session.updateState(ChatState.AGENDAMENTO_FINALIZADO);
            } catch (Exception e) {
                messagingPort.sendMessage(session.getJid(), "Erro ao registrar no Google Calendar. Não foi possível realizar o agendamento");
            }
        } else {
            messagingPort.sendMessage(session.getJid(), "Para confirmar, digite *SIM*.");
        }
    }
}
