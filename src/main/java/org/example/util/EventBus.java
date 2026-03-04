package org.example.util;

import org.example.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {

    public final Map<Class<? extends Event>, List<Consumer<?>>> listeners;

    public EventBus() {
        listeners = new HashMap<>();
    }

    public <T extends Event> void subscribe(Class<T> eventType, Consumer<T> consumer) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(consumer);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        listeners.forEach((eventType, eventListeners) -> {
            if (eventType.isInstance(event)) {
                eventListeners.forEach(consumer -> {
                    ((Consumer<T>) consumer).accept(event);
                });
            }
        });
    }
}
