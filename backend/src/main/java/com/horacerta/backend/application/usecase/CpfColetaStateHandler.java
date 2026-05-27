package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.CalendarPort;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CpfColetaStateHandler implements StateHandler {

    private final MessagingPort messagingPort;
    private final CalendarPort calendarPort;

    public CpfColetaStateHandler(MessagingPort messagingPort, CalendarPort calendarPort) {
        this.messagingPort = messagingPort;
        this.calendarPort = calendarPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.COLETANDO_CPF;
    }

    @Override
    public void handle(ChatSession session, String message) {
        if (message.equals("RESHOW")) return;

        if (message.replaceAll("\\D", "").length() != 11) {
            messagingPort.sendMessage(session.getJid(), "CPF inválido. Por favor, digite os 11 números do seu CPF:");
            return;
        }

        session.getContext().put("cpf", message);
        
        String nome = (String) session.getContext().get("nomeCompleto");
        String dataHoraStr = (String) session.getContext().get("selectedTime");
        LocalDateTime dataHora = LocalDateTime.parse(dataHoraStr);

        messagingPort.sendMessage(session.getJid(), "Confirmando Agendamento...\n" +
                "Cliente: " + nome + "\n" +
                "CPF: " + message + "\n" +
                "Data: " + dataHora.toString() + "\n\n" +
                "Posso confirmar? Digite 'SIM' para finalizar ou 'VOLTAR' para corrigir.");
        
        session.updateState(ChatState.CONFIRMANDO_AGENDAMENTO);
    }
}
