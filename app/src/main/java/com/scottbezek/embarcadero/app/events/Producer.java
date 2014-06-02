package com.scottbezek.embarcadero.app.events;

/**
 */
public interface Producer<Event> {
    Event produce();
}
