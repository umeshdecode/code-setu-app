package com.cargosetu.notification.event;

/**
 * Published after a new Trip is committed with its default (available) status.
 * Purely a trigger for TripOpenedEventListener, which resolves eligible admin recipients and
 * delegates the actual template render/send/audit work to the existing generic notification
 * pipeline via GenericNotificationEvent.
 */
public record TripOpenedEvent(Long tripId) {
}
