package com.raytheon;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.markdown4j.Markdown4jProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class HelpWindow {
    @FXML private WebView helpWebView;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    protected void load() {
        log.debug("HelpWindow load called");
        WebEngine engine = helpWebView.getEngine();
        URL location = getClass().getResource("/Readme.md");
        try (InputStream is = location.openStream()) {
            String html = new Markdown4jProcessor().process(is);
            engine.loadContent(html);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
