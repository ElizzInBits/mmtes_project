package com.horacerta.backend.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarPort {
    List<LocalDateTime> getAvailableSlots(LocalDateTime start, LocalDateTime end);
    void scheduleEvent(String summary, LocalDateTime start, LocalDateTime end, String attendeeEmail);
}
