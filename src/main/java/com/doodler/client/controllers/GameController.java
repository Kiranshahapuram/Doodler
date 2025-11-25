package com.doodler.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;

public class GameController {

    @FXML private Canvas canvas;
    @FXML private TextArea chatArea;
    @FXML private TextField guessField;
    @FXML private Label timerLabel, wordLabel;

    private GraphicsContext gc;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private boolean amDrawer = false;
    private String username;
    private String hostIP;
    private String gameCode;
    private boolean isHost;

    // ---- called from DoodlerClient.startGame() ----
    public void initializeGame(boolean isHost, String username, String hostIP, String code) {
        this.isHost = isHost;
        this.username = username;
        this.hostIP = hostIP;
        this.gameCode = code;

        setupCanvas();
        connectToServer();
    }

    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(3);
        gc.setStroke(Color.BLACK);
    }

    private void setupCanvas() {
        canvas.setOnMousePressed(e -> {
            if (!amDrawer) return;
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
            sendDraw(e.getX(), e.getY(), false);
        });
        canvas.setOnMouseDragged(e -> {
            if (!amDrawer) return;
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            sendDraw(e.getX(), e.getY(), true);
        });
    }

    private void connectToServer() {
        try {
            socket = new Socket(hostIP, 55555);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write("JOIN:" + gameCode + ":" + username + "\n");
            out.flush();

            Executors.newSingleThreadExecutor().submit(this::listenServer);
            appendChat("[INFO] Connected to server.\n");
        } catch (IOException e) {
            appendChat("[ERROR] Could not connect to server.\n");
            e.printStackTrace();
        }
    }

    private void listenServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String msg = line;
                Platform.runLater(() -> handleServerMessage(msg));
            }
        } catch (IOException e) {
            Platform.runLater(() -> appendChat("[Disconnected]\n"));
        }
    }

    // ------------------ Message Handling ------------------
    private void handleServerMessage(String line) {
        if (line.startsWith("INFO:")) {
            appendChat("[INFO] " + line.substring(5) + "\n");
        } else if (line.startsWith("CHAT:")) {
            String[] p = line.split(":", 3);
            appendChat(p[1] + ": " + p[2] + "\n");
        } else if (line.startsWith("DRAW:")) {
            // DRAW:username:x:y:isDrag:isEraser:color
            String[] p = line.split(":", 3);
            if (p.length < 3) return;
            String[] parts = p[2].split(":");
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                boolean drag = Boolean.parseBoolean(parts[2]);
                boolean isEraser = Boolean.parseBoolean(parts[3]);
                Color color = Color.web(parts[4]);
                drawRemote(x, y, drag, isEraser, color);
            } catch (Exception ignored) {}
        } else if (line.startsWith("ROLE:")) {
            amDrawer = line.substring(5).equalsIgnoreCase("DRAWER");
            appendChat("[SYSTEM] Role: " + (amDrawer ? "Drawer" : "Guesser") + "\n");
        } else if (line.startsWith("SECRET:")) {
            String s = line.substring(7);
            wordLabel.setText(s.replaceAll(".", "_ "));
            appendChat("[SECRET] Word to draw: " + s + "\n");
        } else if (line.startsWith("TIME:")) {
            timerLabel.setText("Time: " + line.substring(5) + "s");
        } else if (line.startsWith("CORRECT:")) {
            String[] p = line.split(":", 4);
            appendChat("[ROUND] " + p[1] + " guessed correctly! (" + p[2] + ")\n");
            guessField.setDisable(true);
        } else if (line.startsWith("DISABLE:")) {
            String who = line.substring(8);
            if (who.equals(username)) guessField.setDisable(true);
        } else if (line.startsWith("ROUND_END:")) {
            appendChat("[ROUND END] " + line + "\n");
        } else if (line.startsWith("SCORES:")) {
            appendChat("[SCORES]\n" + line.substring(7).replace(";;", "\n") + "\n");
        } else {
            appendChat("[RAW] " + line + "\n");
        }
    }

    // ------------------ UI Actions ------------------

    @FXML
    public void onSendGuess() {
        String txt = guessField.getText().trim();
        if (txt.isEmpty()) return;
        try {
            out.write("GUESS:" + txt + "\n");
            out.flush();
            guessField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onClearCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (amDrawer) {
            try {
                out.write("CHAT:[SYSTEM] Drawer cleared the canvas\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendDraw(double x, double y, boolean isDrag) {
        try {
            String payload = x + ":" + y + ":" + isDrag + ":" + false + ":#000000";
            out.write("DRAW:" + payload + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawRemote(double x, double y, boolean drag, boolean isEraser, Color color) {
        gc.setStroke(isEraser ? Color.WHITE : color);
        if (!drag) {
            gc.beginPath();
            gc.moveTo(x, y);
            gc.stroke();
        } else {
            gc.lineTo(x, y);
            gc.stroke();
        }
    }

    private void appendChat(String msg) {
        chatArea.appendText(msg);
    }

    @FXML
    private void onExit() {
        try {
            if (out != null) {
                out.write("LEAVE\n");
                out.flush();
            }
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
