package net.lightoze.errbit;

import net.lightoze.errbit.api.*;
import net.lightoze.errbit.api.Error;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 * @author Vladimir Kulev
 */
public class NoticeBuilder {

    protected String apiKey;
    protected String environment;
    protected LoggingEvent event;

    public NoticeBuilder setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public NoticeBuilder setEnvironment(String environment) {
        this.environment = environment;
        return this;
    }

    public NoticeBuilder setEvent(LoggingEvent event) {
        this.event = event;
        return this;
    }

    public Notice build() {
        Notice notice = new Notice();
        notice.setApiKey(apiKey);
        notice.setVersion("2.3");
        notice.setNotifier(notifier());
        notice.setError(error());
        notice.setServerEnvironment(serverEnvironment());
        notice.setRequest(request());
        notice.setCurrentUser(currentUser());
        return notice;
    }

    public Notifier notifier() {
        Notifier notifier = new Notifier();
        notifier.setName("errbit-java");
        notifier.setVersion("2.3");
        return notifier;
    }

    public net.lightoze.errbit.api.Error error() {
        Error error = new Error();
        Throwable throwable = event.getThrowable();
        if (throwable != null) {
            error.setBacktrace(backtrace(throwable));
            error.setClazz(throwable.getClass().getName());
        } else {
            error.setBacktrace(new Backtrace());
            error.setClazz(event.getLoggerName());
        }
        String msg = event.getRenderedMessage();
        if (StringUtils.isBlank(msg) && throwable != null) {
            msg = throwable.toString();
        }
        error.setMessage(msg);
        return error;
    }

    public Backtrace backtrace(Throwable throwable) {
        Backtrace backtrace = new Backtrace();
        List<Backtrace.Line> lines = backtrace.getLine();
        for (Throwable cause : ExceptionUtils.getThrowables(throwable)) {
            addBacktraceHeader(cause, lines);
            addBacktraceLines(cause, lines);
        }
        return backtrace;
    }

    public void addBacktraceHeader(Throwable cause, List<Backtrace.Line> lines) {
        Backtrace.Line line = new Backtrace.Line();
        if (lines.isEmpty()) {
            line.setMethod("Exception " + cause.toString());
        } else {
            line.setMethod("Caused by: " + cause.toString());
        }
        lines.add(line);
    }

    public void addBacktraceLines(Throwable throwable, List<Backtrace.Line> lines) {
        for (StackTraceElement element : throwable.getStackTrace()) {
            Backtrace.Line line = new Backtrace.Line();
            line.setMethod(String.format("%s.%s", element.getClassName(), element.getMethodName()));
            line.setFile(element.getFileName());
            line.setNumber(String.valueOf(element.getLineNumber()));
            lines.add(line);
        }
    }

    public ServerEnvironment serverEnvironment() {
        ServerEnvironment env = new ServerEnvironment();
        try {
            env.setHostname(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            env.setHostname("unknown");
        }
        env.setEnvironmentName(environment);
        return env;
    }

    public Request request() {
        Request request = new Request();
        request.setComponent(event.getLoggerName());
        return request;
    }

    public CurrentUser currentUser() {
        return null;
    }
}
