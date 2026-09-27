package com.cargosetu.maps.service;

import com.cargosetu.maps.client.ComputedRoute;
import com.cargosetu.maps.config.GoogleMapsProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived in-memory reuse of computed routes, keyed by the exact coordinate
 * sequence. Changing any confirmed point changes the key, so an edited trip is
 * never served its old route. Nothing is persisted.
 */
@Component
public class RouteCache {

    public record Entry(ComputedRoute route, Instant calculatedAt, Instant expiresAt) {
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final GoogleMapsProperties properties;

    public RouteCache(GoogleMapsProperties properties) {
        this.properties = properties;
    }

    public Optional<Entry> get(String key) {
        if (properties.getRouteCacheTtlSeconds() <= 0) {
            return Optional.empty();
        }
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    public Entry put(String key, ComputedRoute route) {
        Instant now = Instant.now();
        Entry entry = new Entry(route, now, now.plusSeconds(Math.max(0, properties.getRouteCacheTtlSeconds())));
        if (properties.getRouteCacheTtlSeconds() <= 0) {
            return entry;
        }
        if (entries.size() >= properties.getRouteCacheMaxEntries()) {
            entries.values().removeIf(e -> e.expiresAt().isBefore(now));
            if (entries.size() >= properties.getRouteCacheMaxEntries()) {
                entries.clear();
            }
        }
        entries.put(key, entry);
        return entry;
    }

    public void clear() {
        entries.clear();
    }
}
