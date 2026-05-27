package com.horacerta.backend.infrastructure.adapter.in.web;

import com.horacerta.backend.infrastructure.adapter.out.evolution.EvolutionApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
@Tag(name = "WhatsApp Connection", description = "Endpoints para gerenciar a conexão com o WhatsApp via Evolution API")
public class WhatsAppController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppController.class);

    private final EvolutionApiClient evolutionApiClient;

    @Value("${evolution.api.token}")
    private String apiKey;

    @Value("${evolution.api.instance:horacerta}")
    private String defaultInstance;

    @Value("${server.port:8081}")
    private String serverPort;

    public WhatsAppController(EvolutionApiClient evolutionApiClient) {
        this.evolutionApiClient = evolutionApiClient;
    }

    @PostMapping("/setup")
    @Operation(
        summary = "Setup completo da instância",
        description = "Cria a instância no Evolution (se não existir) e configura o webhook automaticamente"
    )
    public ResponseEntity<Map<String, Object>> setup() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. Criar instância
            Map<String, Object> body = new HashMap<>();
            body.put("instanceName", defaultInstance);
            body.put("qrcode", true);
            body.put("integration", "WHATSAPP-BAILEYS");

            Map<String, Object> createResponse = evolutionApiClient.createInstance(apiKey, body);
            result.put("instance", createResponse);

        } catch (Exception e) {
            // Instância pode já existir, continua para configurar webhook
            log.warn("Criação de instância retornou: {}. Pode já existir, continuando...", e.getMessage());
            result.put("instance", "já existente ou erro: " + e.getMessage());
        }

        try {
            // 2. Configurar webhook apontando para este backend
            Map<String, Object> webhookResponse = configureWebhook();
            result.put("webhook", webhookResponse);
            result.put("status", "ok");
            result.put("message", "Acesse GET /api/v1/whatsapp/qrcode/" + defaultInstance + " para escanear o QR Code.");
        } catch (Exception e) {
            log.error("Erro ao configurar webhook: ", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/webhook")
    @Operation(
        summary = "Configurar webhook",
        description = "Configura o webhook da instância para apontar para este backend"
    )
    public ResponseEntity<Map<String, Object>> setupWebhook() {
        try {
            Map<String, Object> response = configureWebhook();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao configurar webhook: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    private Map<String, Object> configureWebhook() {
        String webhookUrl = "http://host.docker.internal:" + serverPort + "/api/v1/webhook/evolution";
        Map<String, Object> webhookInner = new HashMap<>();
        webhookInner.put("enabled", true);
        webhookInner.put("url", webhookUrl);
        webhookInner.put("byEvents", false);
        webhookInner.put("base64", false);
        webhookInner.put("events", java.util.List.of("MESSAGES_UPSERT"));
        Map<String, Object> webhookBody = new HashMap<>();
        webhookBody.put("webhook", webhookInner);
        return evolutionApiClient.setWebhook(apiKey, defaultInstance, webhookBody);
    }

    @GetMapping(value = "/qrcode/{instanceName}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(
        summary = "Obter QR Code para conexão",
        description = "Busca o QR Code da Evolution API e renderiza como imagem PNG. A instância deve existir (use /setup primeiro)."
    )
    public ResponseEntity<byte[]> getQrCode(@PathVariable String instanceName) {
        try {
            Map<String, Object> response = evolutionApiClient.connectInstance(apiKey, instanceName);
            log.info("Resposta connectInstance: {}", response);

            String base64Code = (String) response.get("base64");

            if (base64Code == null) {
                log.warn("QR Code não disponível. Instância pode já estar conectada ou não existir.");
                return ResponseEntity.notFound().build();
            }

            if (base64Code.contains(",")) {
                base64Code = base64Code.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Code);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);

        } catch (Exception e) {
            log.error("Erro ao buscar QR Code para instância {}: {}", instanceName, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/instances")
    @Operation(summary = "Listar instâncias", description = "Lista todas as instâncias criadas no Evolution")
    public ResponseEntity<Object> listInstances() {
        try {
            Object response = evolutionApiClient.fetchInstances(apiKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao listar instâncias: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
