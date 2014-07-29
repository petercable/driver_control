package com.raytheon;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
import java.nio.file.Path;
import java.nio.file.Paths;

public class HelpWindow {
    @FXML private WebView helpWebView;
    @FXML private TextField urlField;
    private static org.apache.logging.log4j.Logger log = LogManager.getLogger();

    protected void load() {
        WebEngine engine = helpWebView.getEngine();
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
//        helpWebView.getEngine().getHistory().getEntries().addListener(
//                (ListChangeListener<WebHistory.Entry>) c -> {
//                    c.next();
//                    for (WebHistory.Entry e : c.getRemoved()) {
//                        urlField.setText(e.getUrl());
//                    }
//                    for (WebHistory.Entry e : c.getAddedSubList()) {
//                        urlField.setText(e.getUrl());
//                    }
//                });
        helpWebView.getEngine().getHistory().currentIndexProperty().addListener(
                (observable, oldValue, newValue) -> urlField.setText(
                        helpWebView.getEngine().getHistory().getEntries().get((int) newValue).getUrl())
        );
    }

    public void goBack()
    {
        Platform.runLater(()->helpWebView.getEngine().getHistory().go(-1));
    }

    public void goForward()
    {
        Platform.runLater(()->helpWebView.getEngine().getHistory().go(1));
    }
}
