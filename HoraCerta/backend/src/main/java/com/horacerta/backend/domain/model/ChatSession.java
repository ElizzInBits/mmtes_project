package com.horacerta.backend.domain.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

public class ChatSession {
    private String jid;
    private ChatState currentState;
    private ChatState previousState;
    private Map<String, Object> context = new HashMap<>();
    private LocalDateTime lastInteraction;

    public ChatSession() {}

    public ChatSession(String jid, ChatState currentState, Map<String, Object> context, LocalDateTime lastInteraction) {
        this.jid = jid;
        this.currentState = currentState;
        this.context = context;
        this.lastInteraction = lastInteraction;
    }

    public void updateState(ChatState newState) {
        this.previousState = this.currentState;
        this.currentState = newState;
        this.lastInteraction = LocalDateTime.now();
    }

    public void goBack() {
        if (this.previousState != null) {
            ChatState current = this.currentState;
            this.currentState = this.previousState;
            this.previousState = current;
        }
    }

    // Getters and Setters
    public String getJid() { return jid; }
    public void setJid(String jid) { this.jid = jid; }
    
    public ChatState getCurrentState() { return currentState; }
    public void setCurrentState(ChatState currentState) { this.currentState = currentState; }
    
    public ChatState getPreviousState() { return previousState; }
    public void setPreviousState(ChatState previousState) { this.previousState = previousState; }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    public LocalDateTime getLastInteraction() { return lastInteraction; }
    public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }
}
