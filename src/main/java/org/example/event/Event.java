package org.example.event;

public interface Event {

    record SimpleTaskChanged() implements Event {
    }

    record RegularTaskChanged() implements Event {
    }

}
