package com.doodler.client;

import com.doodler.client.controllers.GameController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;

public class DoodlerClient extends Application {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    String username;
    Label timeLabel = new Label("Time: --");
    TextArea chatArea = new TextArea();
    TextField chatInput = new TextField();
    Canvas canvas = new Canvas(700, 450);
    GraphicsContext gc;
    boolean amDrawer = false;
    ColorPicker colorPicker = new ColorPicker(Color.BLACK);
    ToggleButton eraserBtn = new ToggleButton("Eraser");
    TextField codeField = new TextField();
    TextField ipField = new TextField("localhost");

    public static void main(String[] args) { launch(args); }

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("Doodler");
        stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/doodler/client/fxml/landing.fxml"))));
        stage.show();
    }

    public static void startGame(boolean isHost, String username, String hostIP, String code) {
        try {
            FXMLLoader loader = new FXMLLoader(DoodlerClient.class.getResource("/com/doodler/client/fxml/game.fxml"));
            Scene gameScene = new Scene(loader.load());

            GameController controller = loader.getController();
            controller.initializeGame(isHost, username, hostIP, code);

            primaryStage.setScene(gameScene);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectAndShow(Stage stage, String ip, String code) {
        try {
            socket = new Socket(ip, 55555);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write("JOIN:" + code + ":" + username + "\n"); out.flush();
            Executors.newSingleThreadExecutor().submit(new NetworkListener(in, this));
            buildGameUI(stage);
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Could not connect: " + ex.getMessage());
        }
    }

    private void buildGameUI(Stage stage) {
        BorderPane root = new BorderPane();
        gc = canvas.getGraphicsContext2D();
        clearCanvas();

        VBox left = new VBox(8, canvas, new HBox(8, new Label("Color"), colorPicker, eraserBtn), timeLabel);
        left.setPadding(new Insets(8));

        chatArea.setEditable(false);
        chatArea.setPrefWidth(320);
        chatInput.setOnAction(e -> sendChatOrGuess());
        Button send = new Button("Send");
        send.setOnAction(e -> sendChatOrGuess());
        VBox right = new VBox(8, new Label("Chat / Guess"), chatArea, new HBox(6, chatInput, send));
        right.setPadding(new Insets(8));

        root.setLeft(left);
        root.setRight(right);

        Scene scene = new Scene(root, 1100, 520);
        Platform.runLater(() -> {
            stage.setScene(scene);
            stage.setTitle("Scribble - " + username);
            stage.show();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (!amDrawer) return;
            gc.setStroke(eraserBtn.isSelected() ? Color.WHITE : colorPicker.getValue());
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
            sendDraw(e.getX(), e.getY(), false);
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!amDrawer) return;
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            sendDraw(e.getX(), e.getY(), true);
        });
    }

    void handleServerMessage(String line) {
        if (line.startsWith("INFO:")) appendChat("[INFO] " + line.substring(5) + "\n");
        else if (line.startsWith("CHAT:")) {
            String[] p = line.split(":",3);
            appendChat(p[1] + ": " + p[2] + "\n");
        } else if (line.startsWith("DRAW:")) {
            // DRAW:username:payload (payload = x:y:isDrag:isEraser:color)
            String[] p = line.split(":",3);
            if (p.length < 3) return;
            String payload = p[2];
            String[] f = payload.split(":");
            try {
                double x = Double.parseDouble(f[0]);
                double y = Double.parseDouble(f[1]);
                boolean drag = Boolean.parseBoolean(f[2]);
                boolean isEraser = Boolean.parseBoolean(f[3]);
                Color col = Color.web(f[4]);
                drawRemote(x,y,drag,isEraser,col);
            } catch (Exception ignored) {}
        } else if (line.startsWith("ROLE:")) {
            String r = line.substring(5);
            amDrawer = r.equalsIgnoreCase("DRAWER");
            appendChat("[SYSTEM] Role: " + r + "\n");
        } else if (line.startsWith("SECRET:")) {
            appendChat("[SECRET] " + line.substring(7) + "\n");
        } else if (line.startsWith("TIME:")) {
            timeLabel.setText("Time: " + line.substring(5));
        } else if (line.startsWith("CORRECT:")) {
            // CORRECT:user:word:points
            String[] p = line.split(":",4);
            appendChat("[ROUND] " + p[1] + " guessed correctly! Word: " + p[2] + " Points: " + p[3] + "\n");
        } else if (line.startsWith("DISABLE:")) {
            String who = line.substring(8);
            if (who.equals(username)) {
                // disable this client's guess input
                chatInput.setDisable(true);
                appendChat("[SYSTEM] Your guess box has been disabled (you guessed correctly).\n");
            }
        } else if (line.startsWith("SCORES:")) {
            appendChat("[SCORES]\n" + line.substring(7).replace(";;","\n") + "\n");
        } else if (line.startsWith("PLAYERS:")) {
            appendChat("[PLAYERS] " + line.substring(8) + "\n");
        } else if (line.startsWith("ROUND_START")) {
            appendChat("[ROUND] Round started!\n");
            clearCanvas();
            chatInput.setDisable(false);
        } else appendChat("[RAW] " + line + "\n");
    }

    private void sendDraw(double x, double y, boolean isDrag) {
        String colorHex = toHex(colorPicker.getValue());
        boolean isEraser = eraserBtn.isSelected();
        String payload = x + ":" + y + ":" + isDrag + ":" + isEraser + ":" + colorHex;
        try { out.write("DRAW:" + payload + "\n"); out.flush(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void drawRemote(double x, double y, boolean drag, boolean isEraser, Color color) {
        gc.setStroke(isEraser ? Color.WHITE : color);
        if (!drag) {
            gc.beginPath(); gc.moveTo(x,y); gc.stroke();
        } else { gc.lineTo(x,y); gc.stroke(); }
    }

    private void sendChatOrGuess() {
        String txt = chatInput.getText().trim();
        if (txt.isEmpty()) return;
        try {
            if (txt.startsWith("/chat ")) {
                out.write("CHAT:" + txt.substring(6) + "\n");
            } else {
                out.write("GUESS:" + txt + "\n");
            }
            out.flush();
            chatInput.clear();
        } catch (Exception e) { e.printStackTrace(); }
    }

    void appendChat(String t) { chatArea.appendText(t); }

    private void clearCanvas() { gc.clearRect(0,0,canvas.getWidth(), canvas.getHeight()); }

    private void showAlert(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }

    private static String toHex(Color c) {
        int r = (int)(c.getRed()*255), g=(int)(c.getGreen()*255), b=(int)(c.getBlue()*255);
        return String.format("#%02x%02x%02x", r,g,b);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (out!=null) { try { out.write("LEAVE\n"); out.flush(); } catch (IOException ignored) {} }
        if (socket!=null && !socket.isClosed()) socket.close();
    }
}
