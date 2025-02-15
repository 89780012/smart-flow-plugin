package com.smart.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {
    private static final EventBus instance = new EventBus();
    private final Map<String, List<EventListener>> listeners = new HashMap<>();

    private EventBus() {}

    public static EventBus getInstance() {
        return instance;
    }

    public void register(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void unregister(String eventType) {
        if (listeners.containsKey(eventType)) {
            listeners.remove(eventType);
        }
    }

    public void post(String eventType, Object data) {
        if (listeners.containsKey(eventType)) {
            for (EventListener listener : listeners.get(eventType)) {
                listener.onEvent(data);
            }
        }

    }

    public interface EventListener {
        void onEvent(Object data);
    }
}