package com.doodler.server;

import java.sql.*;
import java.util.*;

public class DBManager {
    private final Connection conn;

    public DBManager(String url, String user, String pass) throws SQLException {
        conn = DriverManager.getConnection(url, user, pass);
    }

    public int createGame(String code, String host) throws SQLException {
        String s = "INSERT INTO games(code, host) VALUES (?, ?)";
        try (PreparedStatement p = conn.prepareStatement(s, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, code);
            p.setString(2, host);
            p.executeUpdate();
            ResultSet rs = p.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Could not create game");
    }

    public void setSecret(int gameId, String secret) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement("UPDATE games SET secret_word=? WHERE id=?")) {
            p.setString(1, secret);
            p.setInt(2, gameId);
            p.executeUpdate();
        }
    }

    public int addPlayer(int gameId, String username, boolean isDrawer) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement(
                "INSERT INTO players(game_id, username, is_drawer, can_guess) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, gameId);
            p.setString(2, username);
            p.setBoolean(3, isDrawer);
            p.setBoolean(4, !isDrawer);
            p.executeUpdate();
            ResultSet rs = p.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Could not add player");
    }

    public void addPoints(int playerId, int pts) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement("UPDATE players SET score = score + ? WHERE id=?")) {
            p.setInt(1, pts);
            p.setInt(2, playerId);
            p.executeUpdate();
        }
    }

    public void disableGuessing(int playerId) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement("UPDATE players SET can_guess = false WHERE id=?")) {
            p.setInt(1, playerId);
            p.executeUpdate();
        }
    }

    public Map<String, Integer> getScoresForGame(int gameId) throws SQLException {
        Map<String, Integer> out = new LinkedHashMap<>();
        try (PreparedStatement p = conn.prepareStatement("SELECT username, score FROM players WHERE game_id=? ORDER BY score DESC")) {
            p.setInt(1, gameId);
            ResultSet rs = p.executeQuery();
            while (rs.next()) out.put(rs.getString("username"), rs.getInt("score"));
        }
        return out;
    }

    public void close() throws SQLException { conn.close(); }
}
