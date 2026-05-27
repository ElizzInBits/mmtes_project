package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

@Component
public class IdleStateHandler implements StateHandler {

    private final MessagingPort messagingPort;

    public IdleStateHandler(MessagingPort messagingPort) {
        this.messagingPort = messagingPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.IDLE;
    }

    @Override
    public void handle(ChatSession session, String message) {
        String pushName = (String) session.getContext().getOrDefault("pushName", "cliente");
        String welcomeMessage = "Olá, " + pushName + "! Bem-vindo ao HoraCerta.\n1. Ver Catálogo\n2. Orçamento\n3. Agendar";
        messagingPort.sendMessage(session.getJid(), welcomeMessage);
        session.updateState(ChatState.MENU_PRINCIPAL);
    }
}
