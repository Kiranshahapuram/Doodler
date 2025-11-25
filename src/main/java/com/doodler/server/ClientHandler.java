package com.doodler.server;

import java.io.*;
import java.net.*;

public class ClientHandler {
    final Socket sock;
    final BufferedReader in;
    final BufferedWriter out;
    final String username;
    final GameSession session;

    public ClientHandler(Socket sock, BufferedReader in, BufferedWriter out, String username, GameSession session) {
        this.sock = sock; this.in = in; this.out = out; this.username = username; this.session = session;
    }

    public void listen() {
        try {
            send("INFO:Welcome " + username);
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CHAT:")) {
                    String text = line.substring(5);
                    session.broadcast("CHAT:" + username + ":" + text);
                } else if (line.startsWith("DRAW:")) {
                    // DRAW payload after prefix
                    String payload = line.substring(5);
                    session.handleDraw(this, payload);
                } else if (line.startsWith("GUESS:")) {
                    String guess = line.substring(6);
                    session.checkGuess(this, guess);
                } else if (line.equals("LEAVE")) {
                    break;
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        } finally {
            try { sock.close(); } catch (IOException ignored) {}
            session.removeClient(this);
        }
    }

    public void send(String msg) throws IOException {
        out.write(msg + "\n");
        out.flush();
    }
}
