package com.horacerta.backend.infrastructure.adapter.in.web;

import com.horacerta.backend.application.usecase.ChatbotService;
import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebhookControllerTest {

    private WebhookController controller;

    @Mock
    private ChatbotService chatbotUseCase;

    @Mock
    private ChatSessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new WebhookController(chatbotUseCase, sessionRepository);
        // Injeta a triggerKeyword via reflection (campo @Value não é injetado em testes unitários)
        try {
            var field = WebhookController.class.getDeclaredField("triggerKeyword");
            field.setAccessible(true);
            field.set(controller, "/teste");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldReturn200ForAnyPayload() {
        ResponseEntity<Void> response = controller.handleWebhook(Map.of());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void shouldIgnoreNonMessageUpsertEvents() {
        Map<String, Object> payload = Map.of("event", "connection.update");

        controller.handleWebhook(payload);

        verifyNoInteractions(chatbotUseCase);
    }

    @Test
    void shouldIgnoreMessagesFromMe() {
        Map<String, Object> payload = buildPayload("oi", true, "55199@s.whatsapp.net", "User");

        controller.handleWebhook(payload);

        verifyNoInteractions(chatbotUseCase);
    }

    @Test
    void shouldIgnoreMessageWithoutTriggerWhenNoSession() {
        when(sessionRepository.findByJid("55199@s.whatsapp.net")).thenReturn(Optional.empty());

        Map<String, Object> payload = buildPayload("oi tudo bem", false, "55199@s.whatsapp.net", "User");

        controller.handleWebhook(payload);

        verifyNoInteractions(chatbotUseCase);
    }

    @Test
    void shouldActivateChatbotWhenTriggerKeywordSentWithoutSession() {
        when(sessionRepository.findByJid("55199@s.whatsapp.net")).thenReturn(Optional.empty());

        Map<String, Object> payload = buildPayload("/teste", false, "55199@s.whatsapp.net", "User");

        controller.handleWebhook(payload);

        verify(chatbotUseCase).processMessage(eq("55199@s.whatsapp.net"), eq("/teste"), eq("User"));
    }

    @Test
    void shouldProcessAnyMessageWhenSessionIsActive() {
        ChatSession session = new ChatSession();
        session.setJid("55199@s.whatsapp.net");
        session.setCurrentState(ChatState.MENU_PRINCIPAL);
        session.setContext(new HashMap<>());
        when(sessionRepository.findByJid("55199@s.whatsapp.net")).thenReturn(Optional.of(session));

        Map<String, Object> payload = buildPayload("2", false, "55199@s.whatsapp.net", "User");

        controller.handleWebhook(payload);

        verify(chatbotUseCase).processMessage(eq("55199@s.whatsapp.net"), eq("2"), eq("User"));
    }

    @Test
    void shouldProcessSairToEndSessionEvenWithActiveSession() {
        ChatSession session = new ChatSession();
        session.setJid("55199@s.whatsapp.net");
        session.setCurrentState(ChatState.COLETANDO_NOME);
        session.setContext(new HashMap<>());
        when(sessionRepository.findByJid("55199@s.whatsapp.net")).thenReturn(Optional.of(session));

        Map<String, Object> payload = buildPayload("sair", false, "55199@s.whatsapp.net", "User");

        controller.handleWebhook(payload);

        verify(chatbotUseCase).processMessage(eq("55199@s.whatsapp.net"), eq("sair"), eq("User"));
    }

    @Test
    void shouldReturn200EvenOnInternalError() {
        when(sessionRepository.findByJid(any())).thenThrow(new RuntimeException("DB error"));

        Map<String, Object> payload = buildPayload("oi", false, "55199@s.whatsapp.net", "User");

        ResponseEntity<Void> response = controller.handleWebhook(payload);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPayload(String text, boolean fromMe, String remoteJid, String pushName) {
        Map<String, Object> key = new HashMap<>();
        key.put("remoteJid", remoteJid);
        key.put("fromMe", fromMe);

        Map<String, Object> message = new HashMap<>();
        message.put("conversation", text);

        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("message", message);
        data.put("pushName", pushName);

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "messages.upsert");
        payload.put("data", data);

        return payload;
    }
}
