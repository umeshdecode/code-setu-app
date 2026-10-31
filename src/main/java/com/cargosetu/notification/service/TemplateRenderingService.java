package com.cargosetu.notification.service;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationEventType;

import java.util.Map;

public interface TemplateRenderingService {

    /**
     * Renders the given template string as a notification BODY for the given channel.
     * EMAIL bodies are rendered as Thymeleaf fragments and wrapped in the shared
     * branded HTML layout; all other channels fall back to plain {{var}} substitution.
     *
     * @param template  the template text (plain {{var}} placeholders, or Thymeleaf markup for EMAIL)
     * @param variables the map containing key-value pairs for substitution
     * @param channel   the notification channel this template is rendered for
     * @return the rendered text/HTML
     */
    String render(String template, Map<String, Object> variables, NotificationChannel channel);

    /**
     * Renders the given template string as a notification SUBJECT for the given channel.
     * Unlike {@link #render(String, Map, NotificationChannel)}, this never wraps the
     * result in the branded HTML layout (a subject line is always short, plain text) —
     * for EMAIL it still resolves {{var}} placeholders and any Thymeleaf expressions,
     * just without the surrounding page shell.
     *
     * @param template  the subject template text
     * @param variables the map containing key-value pairs for substitution
     * @param channel   the notification channel this template is rendered for
     * @return the rendered subject text
     */
    String renderSubject(String template, Map<String, Object> variables, NotificationChannel channel);

    /**
     * Legacy entry point: always performs plain {{var}} substitution regardless of channel.
     *
     * @deprecated use {@link #render(String, Map, NotificationChannel)} so EMAIL gets HTML rendering.
     */
    @Deprecated
    String render(String template, Map<String, Object> variables);

    /**
     * Renders an EMAIL body for an event that has no active DB template, using an on-disk
     * fallback template under {@code classpath:templates/email/events/}. Tries
     * {@code <eventType-lowercase>.html} first; if that file doesn't exist, falls back to
     * the common {@code fallback.html} (which displays the event name and the supplied
     * variables generically). The result is wrapped in the same shared branded layout
     * used by {@link #render(String, Map, NotificationChannel)} for EMAIL.
     *
     * @param eventType the event that has no DB template
     * @param variables the event's variables
     * @return the rendered, layout-wrapped HTML body
     */
    String renderEventFileTemplate(NotificationEventType eventType, Map<String, Object> variables);
}
