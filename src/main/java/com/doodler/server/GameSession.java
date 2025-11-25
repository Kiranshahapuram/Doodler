package com.doodler.server;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class GameSession {
    private final String code;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<String> words = Arrays.asList("apple","house","tree","train","dog","cat","flower","phone","book","car");
    private String secret;
    private int timeLeft;
    private boolean roundActive = false;
    private ClientHandler drawer;
    private final DBManager db;
    private int gameId;
    private final Map<ClientHandler,Integer> playerDbIds = new ConcurrentHashMap<>();

    public GameSession(String code, DBManager db) throws Exception {
        this.code = code; this.db = db;
        // create DB entry; host will be updated when first client joins
        this.gameId = db.createGame(code, "unknown");
        System.out.println("Game created in DB id=" + gameId + " code=" + code);
    }

    public void addClient(ClientHandler ch) throws IOException {
        clients.add(ch);
        broadcast("INFO:" + ch.username + " joined the game.");
        updatePlayerList();
        boolean wasFirst = clients.size() == 1;
        if (wasFirst) {
            drawer = ch;
            ch.send("ROLE:DRAWER");
            try {
                int pid = db.addPlayer(gameId, ch.username, true);
                playerDbIds.put(ch, pid);
            } catch (Exception ex) { ex.printStackTrace(); }
            ch.send("INFO:You are the drawer. Secret will be sent to you.");
            startRound();
        } else {
            ch.send("ROLE:GUESser");
            try {
                int pid = db.addPlayer(gameId, ch.username, false);
                playerDbIds.put(ch, pid);
            } catch (Exception ex) { ex.printStackTrace(); }
            ch.send("INFO:Wait for drawer to draw.");
        }
    }

    public void removeClient(ClientHandler ch) {
        clients.remove(ch);
        broadcast("INFO:" + ch.username + " left.");
        updatePlayerList();
        if (ch == drawer) {
            broadcast("INFO:Drawer left. Ending round.");
            endRound();
            if (!clients.isEmpty()) {
                drawer = clients.get(0);
                try { drawer.send("ROLE:DRAWER"); } catch (IOException ignored) {}
            }
        }
    }

    private void updatePlayerList() {
        StringBuilder sb = new StringBuilder("PLAYERS:");
        synchronized (clients) {
            for (ClientHandler c : clients) sb.append(c.username).append(",");
        }
        broadcast(sb.toString());
    }

    private void startRound() {
        if (roundActive) return;
        roundActive = true;
        secret = words.get(new Random().nextInt(words.size()));
        timeLeft = 60;
        broadcast("ROUND_START");
        broadcast("TIME:" + timeLeft);
        // send secret only to drawer
        if (drawer != null) {
            try { drawer.send("SECRET:" + secret); } catch (IOException e) { e.printStackTrace(); }
        }
        try {
            db.setSecret(gameId, secret);
        } catch (Exception ex) { ex.printStackTrace(); }
        scheduler.scheduleAtFixedRate(() -> {
            timeLeft--;
            broadcast("TIME:" + timeLeft);
            if (timeLeft <= 0) {
                broadcast("ROUND_END:TimeUp:" + secret);
                roundActive = false;
                endRound();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void endRound() {
        roundActive = false;
        // print final scores and announce winner
        try {
            Map<String,Integer> scores = db.getScoresForGame(gameId);
            broadcast("ROUND_END:Finished");
            StringBuilder sb = new StringBuilder("SCORES:");
            scores.forEach((u,s) -> sb.append(u).append(",").append(s).append(";;"));
            broadcast(sb.toString());
            // server console:
            System.out.println("Final scores for game " + code + ": " + scores);
        } catch (Exception ex) { ex.printStackTrace(); }
        scheduler.shutdownNow();
    }

    public void checkGuess(ClientHandler ch, String guess) {
        // ignore drawer guesses
        if (ch == drawer) {
            try { ch.send("INFO:Drawer cannot guess."); } catch (IOException ignored) {}
            return;
        }
        if (!roundActive) return;
        if (guess.trim().equalsIgnoreCase(secret.trim())) {
            int points = Math.max(5, timeLeft / 2 + 5); // simple points formula
            broadcast("CORRECT:" + ch.username + ":" + secret + ":" + points);
            // update DB: award points and disable that player's guessing
            try {
                int pid = playerDbIds.getOrDefault(ch, -1);
                if (pid != -1) {
                    db.addPoints(pid, points);
                    db.disableGuessing(pid);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            // disable on clients
            broadcast("DISABLE:" + ch.username);
            // end round after correct guess
            roundActive = false;
            endRound();
        } else {
            broadcast("CHAT:" + ch.username + ":" + guess);
        }
    }

    public void handleDraw(ClientHandler from, String payload) {
        // forward draw to all except origin (so others see)
        synchronized (clients) {
            for (ClientHandler c : clients) {
                if (c != from) {
                    try { c.send("DRAW:" + from.username + ":" + payload); } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }
    }

    public void broadcast(String msg) {
        synchronized (clients) {
            for (ClientHandler c : clients) {
                try { c.send(msg); } catch (IOException e) { e.printStackTrace(); }
            }
        }
        System.out.println("Broadcast [" + code + "]: " + msg);
    }
}
