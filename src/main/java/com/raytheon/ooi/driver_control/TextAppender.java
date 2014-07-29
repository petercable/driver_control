package com.raytheon.ooi.driver_control;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.Serializable;

@Plugin(name = "TextAppender", category = "Core", elementType = "appender", printObject = true)
public class TextAppender extends AbstractAppender {

    private TextAppender(Layout<? extends Serializable> layout, Filter filter, String name) {
        super(name, filter, layout, false);
    }

    @Override
    public void append(LogEvent event) {
        //System.out.println("TEXTAPPENDER " + event.getMessage().getFormattedMessage());
    }

    @PluginFactory
    public static TextAppender createAppender(@PluginElement("Layout") Layout<? extends Serializable> layout,
                                              @PluginElement("Filter") final Filter filter,
                                              @PluginAttribute("name") final String name) {

        return new TextAppender(layout, filter, name);
    }
}
