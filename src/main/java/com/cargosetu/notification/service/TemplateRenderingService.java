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
     * A fully-rendered EMAIL subject + body pair, ready to send.
     */
    record RenderedNotification(String subject, String body) {
    }

    /**
     * Renders the EMAIL subject and body for an event from its on-disk template under
     * {@code classpath:templates/email/events/<event-lowercase>.html}. The file's first
     * line, if it starts with {@code Subject:}, is used as the subject template; the rest
     * is the body template. If no file exists for the event, falls back to the common
     * {@code fallback.html} (which displays the event name and supplied variables
     * generically) with a generic humanized subject. The body is wrapped in the shared
     * branded layout; the subject never is.
     *
     * @param eventType the event to render a template for
     * @param variables the event's variables
     * @return the rendered subject and layout-wrapped HTML body
     */
    RenderedNotification renderEmailForEvent(NotificationEventType eventType, Map<String, Object> variables);
}
