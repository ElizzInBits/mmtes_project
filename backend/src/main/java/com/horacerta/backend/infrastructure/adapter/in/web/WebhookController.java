package com.horacerta.backend.infrastructure.adapter.in.web;

import com.horacerta.backend.application.usecase.ChatbotUseCase;
import com.horacerta.backend.domain.port.out.ChatSessionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
@Tag(name = "Webhook", description = "Recebe eventos da Evolution API")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final ChatbotUseCase chatbotUseCase;
    private final ChatSessionRepository sessionRepository;

    // Palavra-chave que ativa o chatbot quando não há sessão ativa
    @org.springframework.beans.factory.annotation.Value("${chatbot.trigger.keyword:/teste}")
    private String triggerKeyword;

    public WebhookController(ChatbotUseCase chatbotUseCase, ChatSessionRepository sessionRepository) {
        this.chatbotUseCase = chatbotUseCase;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping("/evolution")
    @Operation(summary = "Receber Webhook da Evolution API", description = "Endpoint que processa as mensagens recebidas do WhatsApp")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            String event = (String) payload.get("event");

            if ("messages.upsert".equals(event)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) data.get("message");
                @SuppressWarnings("unchecked")
                Map<String, Object> key = (Map<String, Object>) data.get("key");

                String remoteJid = (String) key.get("remoteJid");
                String pushName = (String) data.get("pushName");

                // Ignora mensagens enviadas pelo próprio bot
                if (Boolean.TRUE.equals(key.get("fromMe"))) {
                    return ResponseEntity.ok().build();
                }

                String text = (String) message.get("conversation");
                if (text == null && message.containsKey("extendedTextMessage")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> extended = (Map<String, Object>) message.get("extendedTextMessage");
                    text = (String) extended.get("text");
                }

                if (text == null) {
                    return ResponseEntity.ok().build();
                }

                // Verifica se já existe uma sessão ativa para este contato
                boolean hasActiveSession = sessionRepository.findByJid(remoteJid).isPresent();

                if (hasActiveSession) {
                    // Sessão ativa: processa normalmente (inclusive "sair"/"cancelar")
                    log.info("Sessão ativa para {}. Processando mensagem.", remoteJid);
                    chatbotUseCase.processMessage(remoteJid, text, pushName);
                } else if (text.trim().equalsIgnoreCase(triggerKeyword)) {
                    // Sem sessão ativa: só inicia se for a palavra-chave
                    log.info("Palavra-chave '{}' recebida de {}. Iniciando chatbot.", triggerKeyword, remoteJid);
                    chatbotUseCase.processMessage(remoteJid, text, pushName);
                } else {
                    // Sem sessão e sem palavra-chave: ignora silenciosamente
                    log.debug("Mensagem ignorada de {} (sem sessão ativa e sem palavra-chave): {}", remoteJid, text);
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Erro ao processar webhook: ", e);
            return ResponseEntity.ok().build();
        }
    }
}
