package com.horacerta.backend.domain.port.out;

import java.util.List;
import java.util.Map;

public interface MessagingPort {
    void sendMessage(String to, String text);
    void sendButtons(String to, String text, List<Map<String, String>> buttons);
    void sendList(String to, String title, String text, String buttonText, List<Map<String, Object>> sections);
}
