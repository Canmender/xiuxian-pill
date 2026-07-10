package com.xiuxian.pill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlchemistManager {

    private final Map<UUID, AlchemistData> cache = new HashMap<>();
    private final File dataDir;

    private static final int[] LEVEL_XP = {0, 1000, 5000, 20000, 80000, 300000, 1000000, 3500000, 12000000, 50000000};
    private static final double[] LEVEL_BONUS = {0.0, 0.05, 0.10, 0.18, 0.28, 0.40, 0.55, 0.72, 0.90, 1.0};
    private static final String[] LEVEL_NAME = {"", "\u4e00\u54c1\u5b66\u5f92", "\u4e8c\u54c1\u70bc\u7ae5", "\u4e09\u54c1\u70bc\u58eb", "\u56db\u54c1\u70bc\u5e08", "\u4e94\u54c1\u5927\u70bc\u5e08", "\u516d\u54c1\u5b97\u5e08", "\u4e03\u54c1\u5927\u5b97\u5e08", "\u516b\u54c1\u4e39\u738b", "\u4e5d\u54c1\u4e39\u5723"};

    public AlchemistManager(File dataFolder) {
        this.dataDir = new File(dataFolder, "alchemist");
        if (!dataDir.exists()) dataDir.mkdirs();
    }

    public AlchemistData getData(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);
        AlchemistData data = load(uuid);
        cache.put(uuid, data);
        return data;
    }

    public void addXp(Player p, int amount) {
        AlchemistData data = getData(p.getUniqueId());
        data.xp += amount;
        int oldLevel = data.level;
        while (data.level < 9 && data.xp >= LEVEL_XP[data.level + 1]) {
            data.xp -= LEVEL_XP[data.level + 1];
            data.level++;
        }
        if (data.level > 9) data.level = 9;
        save(p.getUniqueId(), data);
        if (data.level > oldLevel) {
            p.sendMessage("\u00a76\u00a7l\u3010\u70bc\u4e39\u5e08\u3011\u00a77\u4f60\u7684\u70bc\u4e39\u5e08\u7b49\u7ea7\u63d0\u5347\u5230\u00a7e" + getName(data.level) + "\u00a77\uff01");
        }
    }

    public double getBonus(UUID uuid) {
        AlchemistData data = getData(uuid);
        return LEVEL_BONUS[data.level];
    }

    public String getName(int level) {
        return level >= 0 && level < LEVEL_NAME.length ? LEVEL_NAME[level] : "\u672a\u77e5";
    }

    public String getName(UUID uuid) {
        return getName(getData(uuid).level);
    }

    public int getLevel(UUID uuid) {
        return getData(uuid).level;
    }

    public int getXp(UUID uuid) {
        return getData(uuid).xp;
    }

    public int getXpToNext(UUID uuid) {
        int lv = getLevel(uuid);
        return lv >= 9 ? 0 : LEVEL_XP[lv + 1];
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    private AlchemistData load(UUID uuid) {
        File file = new File(dataDir, uuid.toString() + ".json");
        if (!file.exists()) return new AlchemistData(1, 0);
        try {
            JsonObject json = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            int level = json.has("level") ? json.get("level").getAsInt() : 1;
            int xp = json.has("xp") ? json.get("xp").getAsInt() : 0;
            return new AlchemistData(level, xp);
        } catch (Exception e) {
            return new AlchemistData(1, 0);
        }
    }

    private void save(UUID uuid, AlchemistData data) {
        File file = new File(dataDir, uuid.toString() + ".json");
        try {
            JsonObject json = new JsonObject();
            json.addProperty("level", data.level);
            json.addProperty("xp", data.xp);
            FileWriter writer = new FileWriter(file);
            writer.write(json.toString());
            writer.close();
        } catch (Exception e) {}
    }

    public static class AlchemistData {
        public int level;
        public int xp;
        public AlchemistData(int level, int xp) {
            this.level = level;
            this.xp = xp;
        }
    }
}