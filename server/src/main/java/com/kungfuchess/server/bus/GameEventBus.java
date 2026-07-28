package com.kungfuchess.server.bus;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The pub/sub bus: every event above (score, move log, game start/end) is published here.
 * Deliberately thin -- it's a wrapper over Spring's own {@link ApplicationEventPublisher} rather
 * than a hand-rolled dispatcher, so subscribing is just a {@code @EventListener} method on any
 * Spring bean. Kept as its own type (rather than injecting {@link ApplicationEventPublisher}
 * directly everywhere) so the transport is swappable later without touching publishers.
 */
@Component
public class GameEventBus {

    private final ApplicationEventPublisher publisher;

    public GameEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}
