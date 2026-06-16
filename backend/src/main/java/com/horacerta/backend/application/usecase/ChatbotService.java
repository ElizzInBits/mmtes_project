package com.horacerta.backend.application.usecase;

public interface ChatbotService {
    void processMessage(String jid, String message, String pushName);
}
