package com.horacerta.backend.infrastructure.adapter.out.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.horacerta.backend.domain.port.out.CalendarPort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class GoogleCalendarAdapter implements CalendarPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarAdapter.class);
    private static final String APPLICATION_NAME = "HoraCerta Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.calendar.id}")
    private String calendarId;

    private Calendar service;

    @PostConstruct
    public void init() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        InputStream in = GoogleCalendarAdapter.class.getResourceAsStream("/google-credentials.json");
        if (in == null) {
            log.error("Arquivo google-credentials.json não encontrado em src/main/resources");
            return;
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
        
        this.service = new Calendar.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
        
        log.info("Google Calendar Adapter inicializado com sucesso para a agenda: {}", calendarId);
    }

    @Override
    public List<LocalDateTime> getAvailableSlots(LocalDateTime start, LocalDateTime end) {
        try {
            com.google.api.client.util.DateTime timeMin = new com.google.api.client.util.DateTime(
                    ZonedDateTime.of(start, ZoneId.systemDefault()).toInstant().toEpochMilli());
            com.google.api.client.util.DateTime timeMax = new com.google.api.client.util.DateTime(
                    ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant().toEpochMilli());

            FreeBusyRequest request = new FreeBusyRequest();
            request.setTimeMin(timeMin);
            request.setTimeMax(timeMax);
            request.setItems(Collections.singletonList(new FreeBusyRequestItem().setId(calendarId)));

            FreeBusyResponse response = service.freebusy().query(request).execute();
            List<TimePeriod> busyPeriods = response.getCalendars().get(calendarId).getBusy();

            List<LocalDateTime> availableSlots = new ArrayList<>();
            LocalDateTime current = start.withHour(9).withMinute(0).withSecond(0).withNano(0);
            
            while (current.isBefore(end)) {
                if (current.isAfter(start)) {
                    boolean isBusy = false;
                    for (TimePeriod busy : busyPeriods) {
                        LocalDateTime busyStart = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(busy.getStart().getValue()), ZoneId.systemDefault());
                        LocalDateTime busyEnd = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(busy.getEnd().getValue()), ZoneId.systemDefault());
                        
                        if (current.isBefore(busyEnd) && current.plusHours(1).isAfter(busyStart)) {
                            isBusy = true;
                            break;
                        }
                    }
                    if (!isBusy) {
                        availableSlots.add(current);
                    }
                }
                current = current.plusHours(1);
                
                if (current.getHour() >= 18) {
                    current = current.plusDays(1).withHour(9);
                }
            }

            return availableSlots;

        } catch (IOException e) {
            log.error("Erro ao buscar disponibilidade no Google Calendar", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void scheduleEvent(String summary, LocalDateTime start, LocalDateTime end, String attendeeEmail) {
        try {
            Event event = new Event()
                    .setSummary(summary)
                    .setDescription("Agendamento realizado via WhatsApp (HoraCerta)");

            com.google.api.client.util.DateTime startDateTime = new com.google.api.client.util.DateTime(
                    ZonedDateTime.of(start, ZoneId.systemDefault()).toInstant().toEpochMilli());
            EventDateTime startEvent = new EventDateTime().setDateTime(startDateTime);
            event.setStart(startEvent);

            com.google.api.client.util.DateTime endDateTime = new com.google.api.client.util.DateTime(
                    ZonedDateTime.of(end, ZoneId.systemDefault()).toInstant().toEpochMilli());
            EventDateTime endEvent = new EventDateTime().setDateTime(endDateTime);
            event.setEnd(endEvent);

            service.events().insert(calendarId, event).execute();
            log.info("Evento agendado com sucesso: {} em {}", summary, start);

        } catch (IOException e) {
            log.error("Erro ao agendar evento no Google Calendar", e);
            throw new RuntimeException("Falha ao registrar agendamento no Google.");
        }
    }
}
