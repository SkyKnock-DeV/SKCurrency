package saki4.skcurrency;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

public class SKCurrency extends JavaPlugin implements CommandExecutor {

    private Connection connection;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupDatabase();
        getCommand("skd").setExecutor(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SKExpansion(this).register();
            getLogger().info("PlaceholderAPI успешно подключен!");
        }
    }

    private void setupDatabase() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "/Date.db");
            try (Statement s = connection.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, balance BIGINT DEFAULT 0)");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public long getBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("balance");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0L;
    }

    public void setBalance(UUID uuid, long amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO players (uuid, balance) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET balance = ?")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, Math.max(0, amount));
            ps.setLong(3, Math.max(0, amount));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Map.Entry<String, Long>> getTop() {
        List<Map.Entry<String, Long>> topList = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, balance FROM players WHERE balance > 0 ORDER BY balance DESC LIMIT 10")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("uuid")));
                String name = (op.getName() != null) ? op.getName() : "Unknown";
                topList.add(new AbstractMap.SimpleEntry<>(name, rs.getLong("balance")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return topList;
    }

    private String msg(String path) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("prefix", "") + getConfig().getString("messages." + path, ""));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                reloadConfig();
                sender.sendMessage(msg("reload"));
            }
            case "look" -> {
                if (args.length < 2) return false;
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                sender.sendMessage(msg("balance-look").replace("{player}", args[1]).replace("{amount}", String.valueOf(getBalance(target.getUniqueId()))));
            }
            case "give" -> {
                if (args.length < 3) return false;
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    long amount = Long.parseLong(args[2]);
                    setBalance(target.getUniqueId(), getBalance(target.getUniqueId()) + amount);
                    sender.sendMessage(msg("balance-gave").replace("{player}", args[1]).replace("{amount}", args[2]));
                } catch (NumberFormatException e) { sender.sendMessage(msg("only-numbers")); }
            }
            case "take" -> {
                if (args.length < 3) return false;
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    long amount = Long.parseLong(args[2]);
                    setBalance(target.getUniqueId(), getBalance(target.getUniqueId()) - amount);
                    sender.sendMessage(msg("balance-taken").replace("{player}", args[1]).replace("{amount}", args[2]));
                } catch (NumberFormatException e) { sender.sendMessage(msg("only-numbers")); }
            }
            case "reset" -> {
                if (args.length < 2) return false;
                setBalance(Bukkit.getOfflinePlayer(args[1]).getUniqueId(), 0);
                sender.sendMessage(msg("balance-reset").replace("{player}", args[1]));
            }
            case "resetall" -> {
                try (Statement s = connection.createStatement()) {
                    s.execute("DELETE FROM players");
                    sender.sendMessage(msg("reset-all"));
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
        return true;
    }

    @Override
    public void onDisable() {
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}