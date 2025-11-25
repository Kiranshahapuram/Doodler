package com.doodler.client;

import javafx.application.Platform;
import java.io.BufferedReader;

public class NetworkListener implements Runnable {
    private final BufferedReader in;
    private final DoodlerClient app;

    public NetworkListener(BufferedReader in, DoodlerClient app) { this.in = in; this.app = app; }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String msg = line;
                Platform.runLater(() -> app.handleServerMessage(msg));
            }
        } catch (Exception e) {
            Platform.runLater(() -> app.appendChat("[Disconnected from server]\n"));
        }
    }
}
