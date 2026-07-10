package com.xiuxian.pill;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
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
            case "balance":
            case "money":
            case "lingshi":
                try {
                    org.bukkit.plugin.Plugin ecoPlugin = Bukkit.getPluginManager().getPlugin("XiuXianEco");
                    if (ecoPlugin != null) {
                        com.xiuxian.eco.EcoManager eco = ((com.xiuxian.eco.XiuXianEco) ecoPlugin).getEcoManager();
                        double bal = eco.getBalance(player, "lingshi");
                        return String.valueOf((long) bal);
                    }
                } catch (Exception e) {}
                return "0";
            case "xianyuan":
                try {
                    org.bukkit.plugin.Plugin ecoPlugin2 = Bukkit.getPluginManager().getPlugin("XiuXianEco");
                    if (ecoPlugin2 != null) {
                        com.xiuxian.eco.EcoManager eco2 = ((com.xiuxian.eco.XiuXianEco) ecoPlugin2).getEcoManager();
                        double bal2 = eco2.getBalance(player, "xianyuan");
                        return String.valueOf((long) bal2);
                    }
                } catch (Exception e) {}
                return "0";
            default: return null;
        }
    }
}
