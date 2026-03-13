package saki4.skcurrency;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;

public class SKExpansion extends PlaceholderExpansion {
    private final SKCurrency plugin;
    public SKExpansion(SKCurrency plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "sk"; }
    @Override public @NotNull String getAuthor() { return "saki4"; }
    @Override public @NotNull String getVersion() { return "1.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("balance")) {
            return player != null ? String.valueOf(plugin.getBalance(player.getUniqueId())) : "0";
        }

        List<Map.Entry<String, Long>> top = plugin.getTop();

        if (params.startsWith("top_name_")) {
            try {
                int index = Integer.parseInt(params.replace("top_name_", "")) - 1;
                if (index >= 0 && index < top.size()) return top.get(index).getKey();
            } catch (Exception e) { return "---"; }
            return "---";
        }

        if (params.startsWith("top_balance_")) {
            try {
                int index = Integer.parseInt(params.replace("top_balance_", "")) - 1;
                if (index >= 0 && index < top.size()) return String.valueOf(top.get(index).getValue());
            } catch (Exception e) { return "0"; }
            return "0";
        }
        return null;
    }
}