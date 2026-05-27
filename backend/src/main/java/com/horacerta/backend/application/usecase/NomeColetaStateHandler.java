package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

@Component
public class NomeColetaStateHandler implements StateHandler {

    private final MessagingPort messagingPort;

    public NomeColetaStateHandler(MessagingPort messagingPort) {
        this.messagingPort = messagingPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.COLETANDO_NOME;
    }

    @Override
    public void handle(ChatSession session, String message) {
        if (message.equals("RESHOW")) return;
        session.getContext().put("nomeCompleto", message);
        messagingPort.sendMessage(session.getJid(), "Digite seu CPF:");
        session.updateState(ChatState.COLETANDO_CPF);
    }
}
