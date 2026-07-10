package com.xiuxian.pill;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class AlchemistPAPIExpansion extends PlaceholderExpansion {

    private final XiuXianPill plugin;

    public AlchemistPAPIExpansion(XiuXianPill plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "xipill"; }
    @Override public String getAuthor() { return "XiuXianPill"; }
    @Override public String getVersion() { return "1.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        AlchemistManager am = plugin.getAlchemistManager();
        if (am == null) return "";

        switch (identifier) {
            case "alchemist_level": return String.valueOf(am.getLevel(player.getUniqueId()));
            case "alchemist_name": return am.getName(player.getUniqueId());
            case "alchemist_xp": return String.valueOf(am.getXp(player.getUniqueId()));
            case "alchemist_xp_needed": return String.valueOf(am.getXpToNext(player.getUniqueId()));
            case "alchemist_bonus": return String.format("%.0f", am.getBonus(player.getUniqueId()) * 100);
            // Balance from XConomy
            case "balance":
            case "money":
                try {
                    Class<?> apiClass = Class.forName("me.yic.xconomy.api.XConomyAPI");
                    java.lang.reflect.Method getPlayerData = apiClass.getMethod("getPlayerData", java.util.UUID.class);
                    Object playerData = getPlayerData.invoke(null, player.getUniqueId());
                    if (playerData != null) {
                        java.lang.reflect.Method getBalance = playerData.getClass().getMethod("getBalance");
                        java.math.BigDecimal bal = (java.math.BigDecimal) getBalance.invoke(playerData);
                        return bal != null ? bal.toPlainString() : "0";
                    }
                } catch (Exception e) {}
                return "0";
            default: return null;
        }
    }
}
