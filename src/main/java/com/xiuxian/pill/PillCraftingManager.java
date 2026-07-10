package com.xiuxian.pill;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class PillCraftingManager {

    private final Random random = new Random();
    private final Map<String, Double> baseRates = new LinkedHashMap<>();
    private final Map<String, Double> xpMultipliers = new LinkedHashMap<>();
    private final Map<String, String> qualityColors = new LinkedHashMap<>();
    private final int[][] realmCosts = new int[13][2]; // [material, money]

    private static final String[] QUALITY_KEYS = {"white", "green", "blue", "purple", "gold", "red", "black"};
    private static final String[] QUALITY_NAMES = {"\u51e1\u54c1", "\u7075\u54c1", "\u5b9d\u54c1", "\u5723\u54c1", "\u4ed9\u54c1", "\u5929\u54c1", "\u795e\u54c1"};
    private static final String[] QUALITY_COLORS = {"\u00a77", "\u00a7a", "\u00a7b", "\u00a76", "\u00a75", "\u00a7c", "\u00a74"};
    private static final double[] DEFAULT_RATES = {40, 25, 15, 10, 5, 3, 1};
    private static final double[] DEFAULT_XP_MULT = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
    private static final int[] DEFAULT_MAT = {3, 5, 8, 12, 16, 20, 25, 30, 36, 42, 50, 60, 72};
    private static final int[] DEFAULT_MONEY = {100, 300, 800, 2000, 5000, 12000, 30000, 75000, 180000, 400000, 900000, 2000000, 5000000};

    public void loadConfig(ConfigurationSection cfg) {
        // Load quality rates
        for (int i = 0; i < QUALITY_KEYS.length; i++) {
            baseRates.put(QUALITY_KEYS[i], cfg.getDouble("quality-rates." + QUALITY_KEYS[i], DEFAULT_RATES[i]));
            xpMultipliers.put(QUALITY_KEYS[i], cfg.getDouble("quality-xp-multiplier." + QUALITY_KEYS[i], DEFAULT_XP_MULT[i]));
            qualityColors.put(QUALITY_KEYS[i], QUALITY_COLORS[i]);
        }

        // Load realm costs
        ConfigurationSection costSection = cfg.getConfigurationSection("realm-costs");
        for (int i = 0; i < 13; i++) {
            String key = String.valueOf(i);
            if (costSection != null && costSection.contains(key)) {
                realmCosts[i][0] = costSection.getInt(key + ".material", DEFAULT_MAT[i]);
                realmCosts[i][1] = costSection.getInt(key + ".money", DEFAULT_MONEY[i]);
            } else {
                realmCosts[i][0] = DEFAULT_MAT[i];
                realmCosts[i][1] = DEFAULT_MONEY[i];
            }
        }
    }

    public CraftResult craft(int realmIndex, double alchemistBonus) {
        // Adjust rates with alchemist bonus
        Map<String, Double> adjustedRates = new LinkedHashMap<>();
        double total = 0;
        for (String key : QUALITY_KEYS) {
            double rate = baseRates.get(key);
            // Bonus shifts probability from lower to higher tiers
            if (!key.equals("white")) {
                rate += alchemistBonus * baseRates.get(key);
            } else {
                rate -= alchemistBonus * 20; // reduce white chance
            }
            rate = Math.max(0.1, rate);
            adjustedRates.put(key, rate);
            total += rate;
        }

        // Add scrap rate (1%)
        double scrapRate = 1.0;
        total += scrapRate;

        // Random roll
        double roll = random.nextDouble() * total;
        double cumulative = 0;

        // Check scrap first
        cumulative += scrapRate;
        if (roll < cumulative) {
            return new CraftResult(false, -1, 0, 0);
        }

        // Check qualities
        for (int i = 0; i < QUALITY_KEYS.length; i++) {
            cumulative += adjustedRates.get(QUALITY_KEYS[i]);
            if (roll < cumulative) {
                int xpGain = 1 + random.nextInt(10); // 1-10 random XP
                double xpMult = xpMultipliers.get(QUALITY_KEYS[i]);
                int baseXp = getBaseXp(realmIndex);
                int pillXp = (int)(baseXp * xpMult);
                return new CraftResult(true, i, pillXp, xpGain);
            }
        }

        // Fallback (shouldn't happen)
        return new CraftResult(true, 0, getBaseXp(realmIndex), 1 + random.nextInt(10));
    }

    public int getMaterialCost(int realmIndex) {
        return realmCosts[Math.min(realmIndex, 12)][0];
    }

    public int getMoneyCost(int realmIndex) {
        return realmCosts[Math.min(realmIndex, 12)][1];
    }

    private int getBaseXp(int realmIndex) {
        int[] baseXp = {100, 200, 400, 800, 1500, 3000, 5000, 8000, 12000, 20000, 35000, 60000, 100000};
        return baseXp[Math.min(realmIndex, 12)];
    }

    public String getQualityName(int qualityIndex) {
        return qualityIndex >= 0 && qualityIndex < QUALITY_NAMES.length ? QUALITY_NAMES[qualityIndex] : "\u672a\u77e5";
    }

    public String getQualityColor(int qualityIndex) {
        return qualityIndex >= 0 && qualityIndex < QUALITY_COLORS.length ? QUALITY_COLORS[qualityIndex] : "\u00a77";
    }

    public double getXpMult(int index) {
        double[] m = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
        return index >= 0 && index < m.length ? m[index] : 1.0;
    }

    public static class CraftResult {
        public final boolean success;
        public final int qualityIndex; // -1=scrap, 0-6=quality
        public final int pillXp;
        public final int alchemistXp;

        public CraftResult(boolean success, int qualityIndex, int pillXp, int alchemistXp) {
            this.success = success;
            this.qualityIndex = qualityIndex;
            this.pillXp = pillXp;
            this.alchemistXp = alchemistXp;
        }
    }
}