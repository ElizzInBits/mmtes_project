package com.horacerta.backend.application.usecase;

import com.horacerta.backend.domain.model.ChatSession;
import com.horacerta.backend.domain.model.ChatState;
import com.horacerta.backend.domain.port.out.ChatSessionRepository;
import com.horacerta.backend.domain.port.out.MessagingPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatbotUseCaseTest {

    private ChatbotUseCase chatbotUseCase;

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private MessagingPort messagingPort;

    @Mock
    private StateHandler idleHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        List<StateHandler> handlers = new ArrayList<>();
        handlers.add(idleHandler);

        when(idleHandler.getSupportedState()).thenReturn(ChatState.IDLE);

        chatbotUseCase = new ChatbotUseCase(sessionRepository, messagingPort, handlers);
    }

    @Test
    void shouldCreateNewSessionWhenNoneExists() {
        String jid = "123456@s.whatsapp.net";
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.empty());

        chatbotUseCase.processMessage(jid, "Oi", "User");

        // Save é chamado 2x: uma ao criar a sessão (orElseGet) e outra ao final do processamento
        verify(sessionRepository, times(2)).save(any(ChatSession.class));
        verify(idleHandler).handle(any(ChatSession.class), eq("Oi"));
    }

    @Test
    void shouldReuseExistingSession() {
        String jid = "123456@s.whatsapp.net";
        ChatSession existing = sessionWith(jid, ChatState.IDLE);
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(existing));

        chatbotUseCase.processMessage(jid, "Oi", "User");

        verify(sessionRepository, times(1)).save(any(ChatSession.class));
        verify(idleHandler).handle(eq(existing), eq("Oi"));
    }

    @Test
    void shouldResetSessionOnSair() {
        String jid = "123456@s.whatsapp.net";
        ChatSession session = sessionWith(jid, ChatState.MENU_PRINCIPAL);
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(session));

        chatbotUseCase.processMessage(jid, "sair", "User");

        verify(messagingPort).sendMessage(eq(jid), contains("encerrado"));
        verify(sessionRepository).deleteByJid(jid);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void shouldResetSessionOnCancelar() {
        String jid = "123456@s.whatsapp.net";
        ChatSession session = sessionWith(jid, ChatState.COLETANDO_NOME);
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(session));

        chatbotUseCase.processMessage(jid, "cancelar", "User");

        verify(messagingPort).sendMessage(eq(jid), contains("encerrado"));
        verify(sessionRepository).deleteByJid(jid);
    }

    @Test
    void shouldBeCaseInsensitiveForSair() {
        String jid = "123456@s.whatsapp.net";
        ChatSession session = sessionWith(jid, ChatState.MENU_PRINCIPAL);
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(session));

        chatbotUseCase.processMessage(jid, "SAIR", "User");

        verify(sessionRepository).deleteByJid(jid);
    }

    @Test
    void shouldStorePushNameInContextOnFirstMessage() {
        String jid = "123456@s.whatsapp.net";
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.empty());

        chatbotUseCase.processMessage(jid, "Oi", "Carlos");

        verify(idleHandler).handle(argThat(session ->
            "Carlos".equals(session.getContext().get("pushName"))
        ), eq("Oi"));
    }

    @Test
    void shouldNotOverwritePushNameIfAlreadyPresent() {
        String jid = "123456@s.whatsapp.net";
        ChatSession session = sessionWith(jid, ChatState.IDLE);
        session.getContext().put("pushName", "Carlos Original");
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(session));

        chatbotUseCase.processMessage(jid, "Oi", "Novo Nome");

        assertThat(session.getContext().get("pushName")).isEqualTo("Carlos Original");
    }

    @Test
    void shouldGoBackOnVoltar() {
        String jid = "123456@s.whatsapp.net";
        ChatSession session = sessionWith(jid, ChatState.COLETANDO_CPF);
        session.setPreviousState(ChatState.COLETANDO_NOME);

        StateHandler nomeHandler = mock(StateHandler.class);
        when(nomeHandler.getSupportedState()).thenReturn(ChatState.COLETANDO_NOME);

        chatbotUseCase = new ChatbotUseCase(sessionRepository,messagingPort, List.of(idleHandler, nomeHandler));
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(session));

        chatbotUseCase.processMessage(jid, "voltar", "User");

        assertThat(session.getCurrentState()).isEqualTo(ChatState.COLETANDO_NOME);
        verify(nomeHandler).handle(eq(session), eq("RESHOW"));
    }

    @Test
    void shouldSendFallbackMessageOnUnknownState() {
        String jid = "123456@s.whatsapp.net";
        ChatSession session = sessionWith(jid, ChatState.AGENDAMENTO_FINALIZADO);
        when(sessionRepository.findByJid(jid)).thenReturn(Optional.of(session));

        // Nenhum handler suporta AGENDAMENTO_FINALIZADO
        chatbotUseCase.processMessage(jid, "algo", "User");

        verify(messagingPort).sendMessage(eq(jid), contains("Oi"));
    }

    // --- helpers ---

    private ChatSession sessionWith(String jid, ChatState state) {
        ChatSession session = new ChatSession();
        session.setJid(jid);
        session.setCurrentState(state);
        session.setContext(new HashMap<>());
        return session;
    }
}
