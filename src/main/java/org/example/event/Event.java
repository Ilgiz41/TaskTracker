package org.example.event;

import lombok.Getter;

public interface Event {

    interface UserNotificationEvent extends Event {
        String getMessage();
    }

    abstract class TaskUIEvent implements Event {
    }

    class RefreshFullTaskListEvent extends TaskUIEvent {
    }

    class RefreshVirtualTaskListEvent extends TaskUIEvent {
    }

    abstract class ExceptionEvent implements Event {
        @Getter
        private final Throwable cause;
        @Getter
        private final long timestamp;

        public ExceptionEvent(Throwable cause) {
            this.cause = cause;
            this.timestamp = System.currentTimeMillis();
        }
    }

    class NotificationErrorEvent extends ExceptionEvent implements UserNotificationEvent {

        @Getter
        private final String message;

        public NotificationErrorEvent(Throwable cause, String message) {
            super(cause);
            this.message = message;
        }
    }

    class NotificationEvent implements UserNotificationEvent {
        @Getter
        private final String message;

        public NotificationEvent(String message) {
            this.message = message;
        }
    }

    class CriticalErrorExceptionEvent extends ExceptionEvent implements UserNotificationEvent {
        @Getter
        private final String message;

        public CriticalErrorExceptionEvent(Throwable cause, String message) {
            super(cause);
            this.message = message;
        }
    }
}
