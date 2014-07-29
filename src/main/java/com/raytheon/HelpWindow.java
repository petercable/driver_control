package com.raytheon;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.markdown4j.Markdown4jProcessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HelpWindow {
    @FXML private WebView helpWebView;
    @FXML private TextField urlField;
    private WebEngine engine;
    private WebHistory history;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    @FXML
    private void initialize() {
        engine = helpWebView.getEngine();
        history = engine.getHistory();
        history.currentIndexProperty().addListener(
                (observable, oldValue, newValue)
                        -> urlField.setText(history.getEntries().get((int) newValue).getUrl()));
        load();
    }

    protected void load() {
        URL location = getClass().getResource("/Readme.md");
        try ( InputStream is = location.openStream() )
        {
            File homePage = File.createTempFile("Readme", ".html");
            homePage.deleteOnExit();
            String html = new Markdown4jProcessor().process(is);
            Files.write(Paths.get(homePage.toURI()), html.getBytes());
            engine.load(homePage.toURI().toString());
            urlField.setText(homePage.toURI().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goBack()
    {
        Platform.runLater(()->history.go(-1));
    }

    public void goForward()
    {
        Platform.runLater(() -> history.go(1));
    }
}
