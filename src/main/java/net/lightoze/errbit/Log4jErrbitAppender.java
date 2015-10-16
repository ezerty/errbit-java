package net.lightoze.errbit;

import java.io.Serializable;
import net.lightoze.errbit.api.Notice;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * @author Vladimir Kulev
 */
@Plugin(name = "Log4jErrbit", category = "core", elementType = "appender", printObject = true)
public class Log4jErrbitAppender extends AbstractAppender {

    private Class<? extends NoticeBuilder> noticeBuilder = NoticeBuilder.class;
    private NoticeSender sender;
    private String url;
    private String apiKey;
    private String environment;
    private boolean enabled = true;

    protected Log4jErrbitAppender(String name, Filter filter,
            Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    @Override
    public void append(final LogEvent event) {
        if (!enabled) {
            return;
        }
        try {
            NoticeBuilder builder = noticeBuilder.newInstance();
            LoggingEvent loggingEvent = new net.lightoze.errbit.LoggingEvent() {

                @Override
                public Throwable getThrowable() {
                    return event.getThrown();
                }

                @Override
                public String getLoggerName() {
                    return event.getLoggerName();
                }

                @Override
                public String getRenderedMessage() {
                    return event.getMessage().getFormattedMessage();
                }
            };

            Notice notice = builder
                    .setEvent(loggingEvent)
                    .setApiKey(apiKey)
                    .setEnvironment(environment)
                    .build();
            sender.send(notice);
        } catch (Exception e) {
            LOGGER.warn("Could not send error notice", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void setNoticeBuilder(String className) throws ClassNotFoundException {
        noticeBuilder = (Class<? extends NoticeBuilder>) Thread.currentThread().getContextClassLoader().loadClass(className);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void activateOptions() {
        Validate.notNull(url);
        Validate.notNull(apiKey);
        Validate.notNull(environment);
        sender = new NoticeSender(url);
    }

    @PluginFactory
    public static Log4jErrbitAppender createAppender(@PluginAttribute("name") String name,
            @PluginAttribute("url") String url,
            @PluginAttribute("apiKey") String apiKey,
            @PluginAttribute("environment") String environment,
            @PluginElement("Layout") Layout layout,
            @PluginElement("Filters") Filter filter) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        if (name == null) {
            LOGGER.error("No name provided for Log4jErrbitAppender");
            return null;
        }

        Log4jErrbitAppender appender = new Log4jErrbitAppender(name, filter, layout, false);
        appender.setUrl(url);
        appender.setApiKey(apiKey);
        appender.setEnvironment(environment);

        appender.activateOptions();

        return appender;
    }
}
