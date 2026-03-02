package org.example.event;

import lombok.Getter;

public interface Event {

    abstract class TaskUIEvent implements Event{}
    class RefreshFullTaskListEvent extends TaskUIEvent {}
    class RefreshVirtualTaskListEvent extends TaskUIEvent {}

    abstract class ExceptionEvent implements Event{
        @Getter
        private final Throwable cause;
        @Getter
        private final long timestamp;

        public ExceptionEvent(Throwable cause) {
            this.cause = cause;
            this.timestamp = System.currentTimeMillis();
        }
    }

    class UserNotificationErrorEvent extends ExceptionEvent {
        public UserNotificationErrorEvent(Throwable cause) {
            super(cause);
        }
    }

    class CriticalErrorExceptionEvent extends ExceptionEvent {
        public CriticalErrorExceptionEvent(Throwable cause) {
            super(cause);
        }
    }
}
