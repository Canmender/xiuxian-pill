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
            default: return null;
        }
    }
}
