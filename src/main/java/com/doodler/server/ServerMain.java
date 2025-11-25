package com.doodler.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerMain {
    public static final int PORT = 55555;
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final DBManager db;

    public ServerMain(DBManager db) { this.db = db; }

    public static void main(String[] args) throws Exception {
        // DB config - change if needed
        String url = "jdbc:mysql://localhost:3306/doodlerdb?serverTimezone=UTC";
        DBManager db = new DBManager(url, "root", "Harshithaa@mysql08");
        ServerMain m = new ServerMain(db);
        m.start();
    }

    public void start() throws IOException {
        ServerSocket ss = new ServerSocket(PORT);
        System.out.println("Server listening on port " + PORT);
        while (true) {
            Socket s = ss.accept();
            new Thread(() -> handleClient(s)).start();
        }
    }

    private void handleClient(Socket sock) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            String line = in.readLine();
            if (line == null || !line.startsWith("JOIN:")) {
                out.write("ERROR:Bad join\n"); out.flush(); sock.close(); return;
            }
            String[] parts = line.split(":", 3);
            String code = parts[1];
            String username = parts[2];

            GameSession session = sessions.computeIfAbsent(code, c -> {
                try {
                    System.out.println("Creating session " + c);
                    return new GameSession(c, db);
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });

            ClientHandler ch = new ClientHandler(sock, in, out, username, session);
            session.addClient(ch);
            ch.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
