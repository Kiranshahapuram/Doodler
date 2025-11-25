package com.doodler.client.controllers;

import com.doodler.client.DoodlerClient;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.Random;

public class LandingController {

    @FXML private TextField nameField;

    @FXML
    public void onHost() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Please enter your name before hosting.");
            return;
        }

        // Generate random 6-digit code
        String code = String.valueOf(100000 + new Random().nextInt(900000));

        // Create a custom dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Host Game");
        dialog.setHeaderText("Your Game Code");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/doodler/client/styles/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        // Create dialog content
        Label codeLabel = new Label(code);
        codeLabel.setStyle("-fx-font-size: 26px; -fx-text-fill: #202020; -fx-font-weight: bold; "
                + "-fx-background-color: #a6f516; -fx-padding: 6 14; -fx-background-radius: 8;");

        Label infoLabel = new Label("Share this code with your friends so they can join!");
        infoLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");

        Button copyBtn = new Button("Copy Code");
        copyBtn.setStyle("-fx-background-color: #a6f516; -fx-text-fill: #202020; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");

        copyBtn.setOnAction(e -> {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(code);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            copyBtn.setText("Copied!");
            copyBtn.setDisable(true);
        });

        ButtonType startBtn = new ButtonType("Start Game", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(startBtn, cancelBtn);

        VBox content = new VBox(10, codeLabel, infoLabel);
        content.setStyle("-fx-alignment: center; -fx-padding: 15;");
        dialog.getDialogPane().setContent(content);

        // Wait for result
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == startBtn) {
            // ✅ Only start the game when Start is clicked
            DoodlerClient.startGame(true, name, "localhost", code);
        }
    }

    @FXML
    public void onJoin() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Please enter your name before joining.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Game");
        dialog.setHeaderText("Enter Game Code");
        dialog.setContentText("Code:");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/doodler/client/styles/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        dialog.showAndWait().ifPresent(code -> {
            if (!code.trim().isEmpty()) {
                DoodlerClient.startGame(false, name, "localhost", code);
            } else {
                showAlert("Please enter a valid game code.");
            }
        });
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.setTitle("Info");
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/doodler/client/styles/style.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }
}
