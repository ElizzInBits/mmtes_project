package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoStateHandler implements StateHandler {

    private final MessagingPort messagingPort;

    public OrcamentoStateHandler(MessagingPort messagingPort) {
        this.messagingPort = messagingPort;
    }

    @Override
    public ChatState getSupportedState() {
        return ChatState.SOLICITANDO_ORCAMENTO;
    }

    @Override
    public void handle(ChatSession session, String message) {
        if (message.equals("RESHOW")) return;

        messagingPort.sendMessage(session.getJid(), "Recebemos sua solicitação de orçamento: \"" + message + 
                "\". Em breve um de nossos consultores entrará em contato!");
        
        session.updateState(ChatState.IDLE);
    }
}
