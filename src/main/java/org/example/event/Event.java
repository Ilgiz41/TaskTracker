package org.example.event;

public interface Event {

    record TaskCacheChanged() implements Event {
    }

    record VirtualTaskChanged() implements Event {
    }

}
