package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;

public interface StateHandler {
    ChatState getSupportedState();
    void handle(ChatSession session, String message);
}
