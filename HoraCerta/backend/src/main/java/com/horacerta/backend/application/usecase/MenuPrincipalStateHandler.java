package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

@Component
public class MenuPrincipalStateHandler implements StateHandler {

    private final MessagingPort messagingPort;

    public MenuPrincipalStateHandler(MessagingPort messagingPort) {
        this.messagingPort = messagingPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.MENU_PRINCIPAL;
    }

    @Override
    public void handle(ChatSession session, String message) {
        String option = message.trim();
        switch (option) {
            case "1":
                messagingPort.sendMessage(session.getJid(), "Catálogo: [LINK]");
                session.updateState(ChatState.IDLE);
                break;
            case "2":
                messagingPort.sendMessage(session.getJid(), "Descreva o serviço:");
                session.updateState(ChatState.SOLICITANDO_ORCAMENTO);
                break;
            case "3":
                messagingPort.sendMessage(session.getJid(), "Qual data deseja consultar?");
                session.updateState(ChatState.ESCOLHENDO_DATA);
                break;
            default:
                messagingPort.sendMessage(session.getJid(), "Escolha 1, 2 ou 3.");
                break;
        }
    }
}
