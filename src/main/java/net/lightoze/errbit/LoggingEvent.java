package net.lightoze.errbit;

public interface LoggingEvent {

    Throwable getThrowable();

    String getLoggerName();

    String getRenderedMessage();
}
