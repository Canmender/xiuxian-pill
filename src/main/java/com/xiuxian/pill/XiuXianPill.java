package com.xiuxian.pill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.*;

public class XiuXianPill extends JavaPlugin implements CommandExecutor, Listener {

    private final Map<String, PillData> liantiPills = new HashMap<>();
    private final Map<String, PillData> xiufaPills = new HashMap<>();
    private File xiuxianDataDir;

    private AlchemistManager alchemistManager;
    private PillCraftingManager craftManager;
    private AlchemyGUI alchemyGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        loadPills();

        xiuxianDataDir = new File("D:/MineCraft/server/plugins/XiuXianCore/data");

        // Initialize alchemy system
        alchemistManager = new AlchemistManager(getDataFolder());
        craftManager = new PillCraftingManager();
        craftManager.loadConfig(getConfig());
        alchemyGUI = new AlchemyGUI(this);

        // Register PAPI expansions
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AlchemistPAPIExpansion(this).register();
            getLogger().info("PAPI expansion registered");
        }

        getCommand("pill").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(alchemyGUI, this);

        getLogger().info("XiuXianPill v2.0 enabled: " + liantiPills.size() + " lianti, " + xiufaPills.size() + " xiufa pills, alchemy system ready");
    }

    @Override
    public void onDisable() {
        getLogger().info("XiuXianPill disabled");
    }

    // ========== Pill Loading ==========

    private void loadPills() {
        liantiPills.clear();
        xiufaPills.clear();
        FileConfiguration cfg = getConfig();

        ConfigurationSection ltSection = cfg.getConfigurationSection("lianti-pills");
        if (ltSection != null) {
            for (String key : ltSection.getKeys(false)) {
                ConfigurationSection s = ltSection.getConfigurationSection(key);
                if (s == null) continue;
                liantiPills.put("lt" + key, new PillData(
                    Integer.parseInt(key),
                    colorize(s.getString("name", "")),
                    s.getString("description", ""),
                    s.getString("realm", ""),
                    s.getInt("xp-amount", 100),
                    Material.matchMaterial(s.getString("material", "PAPER")),
                    s.getString("alchemist-material", "")
                ));
            }
        }

        ConfigurationSection xfSection = cfg.getConfigurationSection("xiufa-pills");
        if (xfSection != null) {
            for (String key : xfSection.getKeys(false)) {
                ConfigurationSection s = xfSection.getConfigurationSection(key);
                if (s == null) continue;
                xiufaPills.put("xf" + key, new PillData(
                    Integer.parseInt(key),
                    colorize(s.getString("name", "")),
                    s.getString("description", ""),
                    s.getString("realm", ""),
                    s.getInt("xp-amount", 100),
                    Material.matchMaterial(s.getString("material", "PAPER")),
                    s.getString("alchemist-material", "")
                ));
            }
        }
    }

    // ========== Public API ==========

    public Map<String, Map<String, Object>> getAllPills() {
        Map<String, Map<String, Object>> all = new LinkedHashMap<>();
        for (Map.Entry<String, PillData> e : liantiPills.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", e.getValue().name);
            map.put("xp-amount", e.getValue().xpAmount);
            map.put("material", e.getValue().alchemistMaterial);
            map.put("type", "lianti");
            all.put(e.getKey(), map);
        }
        for (Map.Entry<String, PillData> e : xiufaPills.entrySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", e.getValue().name);
            map.put("xp-amount", e.getValue().xpAmount);
            map.put("material", e.getValue().alchemistMaterial);
            map.put("type", "xiufa");
            all.put(e.getKey(), map);
        }
        return all;
    }

    public PillData getPillData(String id) {
        PillData pd = liantiPills.get(id);
        if (pd == null) pd = xiufaPills.get(id);
        return pd;
    }

    public AlchemistManager getAlchemistManager() { return alchemistManager; }
    public PillCraftingManager getCraftManager() { return craftManager; }

    // ========== Commands ==========

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { showHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "list":
                showPillList(sender);
                break;
            case "give":
                givePill(sender, args);
                break;
            case "use":
                if (!(sender instanceof Player)) { sender.sendMessage("Only players"); return true; }
                usePillFromHand((Player) sender);
                break;
            case "craft":
                if (!(sender instanceof Player)) { sender.sendMessage("Only players"); return true; }
                if (!sender.hasPermission("xipill.craft")) { sender.sendMessage("\u00a7c\u6ca1\u6709\u6743\u9650"); return true; }
                alchemyGUI.open((Player) sender);
                break;
            case "alchemist":
                if (!(sender instanceof Player)) { sender.sendMessage("Only players"); return true; }
                showAlchemistInfo((Player) sender);
                break;
            case "reload":
                if (!sender.hasPermission("xipill.admin")) { sender.sendMessage("\u00a7c\u6ca1\u6709\u6743\u9650"); return true; }
                reloadConfig();
                loadPills();
                craftManager.loadConfig(getConfig());
                sender.sendMessage(colorize("\u00a7a\u4e39\u836f\u914d\u7f6e\u5df2\u91cd\u8f7d"));
                break;
            default:
                showHelp(sender);
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(colorize("\u00a76\u00a7l\u2501\u2501\u2501 \u4e39\u836f\u7cfb\u7edf \u2501\u2501\u2501"));
        sender.sendMessage(colorize(" \u00a7e/pill list \u00a77- \u67e5\u770b\u4e39\u836f\u5217\u8868"));
        sender.sendMessage(colorize(" \u00a7e/pill use \u00a77- \u624b\u6301\u670d\u7528"));
        sender.sendMessage(colorize(" \u00a7e/pill craft \u00a77- \u6253\u5f00\u70bc\u4e39\u53f0"));
        sender.sendMessage(colorize(" \u00a7e/pill alchemist \u00a77- \u67e5\u770b\u70bc\u4e39\u5e08\u4fe1\u606f"));
        sender.sendMessage(colorize(" \u00a7e/pill give <\u73a9\u5bb6> <\u7c7b\u578b> [\u6570\u91cf]"));
        sender.sendMessage(colorize(" \u00a7e/pill reload \u00a77- \u91cd\u8f7d\u914d\u7f6e"));
        sender.sendMessage(colorize("\u00a76\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501"));
    }

    private void showAlchemistInfo(Player p) {
        int level = alchemistManager.getLevel(p.getUniqueId());
        int xp = alchemistManager.getXp(p.getUniqueId());
        int xpNeed = alchemistManager.getXpToNext(p.getUniqueId());
        String name = alchemistManager.getName(p.getUniqueId());
        double bonus = alchemistManager.getBonus(p.getUniqueId());

        p.sendMessage(colorize("\u00a76\u00a7l\u2501\u2501\u2501 \u70bc\u4e39\u5e08\u4fe1\u606f \u2501\u2501\u2501"));
        p.sendMessage(colorize(" \u00a77\u7b49\u7ea7: \u00a7e" + name));
        p.sendMessage(colorize(" \u00a77XP: \u00a7e" + xp + " \u00a77/ \u00a7e" + xpNeed));
        p.sendMessage(colorize(" \u00a77\u9ad8\u54c1\u8d28\u52a0\u6210: \u00a7a+" + (int)(bonus * 100) + "%"));
        p.sendMessage(colorize("\u00a76\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501"));
    }

    // ========== Furnace Right-Click ==========

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Check if holding alchemy furnace
        if (meta.hasCustomModelData() && meta.getCustomModelData() == 14001) {
            event.setCancelled(true);
            alchemyGUI.open(p);
            return;
        }

        // Original pill use logic
        if (!meta.hasDisplayName()) return;
        PillData pill = findPill(meta.getDisplayName(), item.getType());
        if (pill != null) {
            event.setCancelled(true);
            usePill(p, pill, item);
        }
    }

    // ========== Original pill methods ==========

    private void showPillList(CommandSender sender) {
        sender.sendMessage(colorize("\u00a76\u00a7l\u2501\u2501\u2501 \u4f53\u4fee\u7ecf\u9a8c\u4e39 \u2501\u2501\u2501"));
        for (Map.Entry<String, PillData> entry : liantiPills.entrySet()) {
            PillData pill = entry.getValue();
            sender.sendMessage(colorize(" \u00a7f" + entry.getKey() + " \u00a77- " + pill.name + " \u00a77(" + pill.realm + ") \u00a7e" + pill.xpAmount + "\u4fee\u4e3a"));
        }
        sender.sendMessage(colorize("\u00a76\u2501\u2501\u2501 \u6cd5\u4fee\u7ecf\u9a8c\u4e39 \u2501\u2501\u2501"));
        for (Map.Entry<String, PillData> entry : xiufaPills.entrySet()) {
            PillData pill = entry.getValue();
            sender.sendMessage(colorize(" \u00a7f" + entry.getKey() + " \u00a77- " + pill.name + " \u00a77(" + pill.realm + ") \u00a7e" + pill.xpAmount + "\u4fee\u4e3a"));
        }
        sender.sendMessage(colorize("\u00a76\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501"));
    }

    private void givePill(CommandSender sender, String[] args) {
        if (!sender.hasPermission("xipill.admin")) { sender.sendMessage("\u00a7c\u6ca1\u6709\u6743\u9650"); return; }
        if (args.length < 3) { sender.sendMessage("\u00a7e\u7528\u6cd5: /pill give <\u73a9\u5bb6> <\u7c7b\u578b> [\u6570\u91cf]"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("\u00a7c\u73a9\u5bb6\u4e0d\u5728\u7ebf: " + args[1]); return; }

        String pillType = args[2].toLowerCase();
        int amount = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        PillData pill = liantiPills.get(pillType);
        if (pill == null) pill = xiufaPills.get(pillType);
        if (pill == null) { sender.sendMessage("\u00a7c\u65e0\u6548\u7684\u4e39\u836f\u7c7b\u578b: " + pillType); return; }

        ItemStack item = createPillItem(pill);
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage(colorize("\u00a7a\u7ed9\u4e88 " + target.getName() + " " + pill.name + " x" + amount));
    }

    private void usePillFromHand(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) { p.sendMessage("\u00a7e\u8bf7\u624b\u6301\u4e39\u836f"); return; }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) { p.sendMessage("\u00a7e\u8fd9\u4e0d\u662f\u4e39\u836f"); return; }
        PillData pill = findPill(meta.getDisplayName(), item.getType());
        if (pill == null) { p.sendMessage("\u00a7e\u8fd9\u4e0d\u662f\u4e39\u836f"); return; }
        usePill(p, pill, item);
    }

    private void useXiuXianItemsPill(Player p, ItemStack item, int cmd) {
        // cmd 20001-20182 -> pill index 0-181
        int idx = cmd - 20001;
        int pillIdx = idx / 7;  // 26 pill types
        int qualityIdx = idx % 7;  // 7 qualities

        String[] pillIds = {"lt0","lt1","lt2","lt3","lt4","lt5","lt6","lt7","lt8","lt9","lt10","lt11","lt12",
                            "xf0","xf1","xf2","xf3","xf4","xf5","xf6","xf7","xf8","xf9","xf10","xf11","xf12"};
        String[] qualityNames = {"凡品","灵品","宝品","圣品","仙品","天品","神品"};
        double[] qualityMulti = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};

        if (pillIdx < 0 || pillIdx >= pillIds.length) return;
        String id = pillIds[pillIdx];
        PillData pd = getPillData(id);
        if (pd == null) return;

        boolean isLianti = id.startsWith("lt");
        int playerRealmIdx = getPlayerRealmIndex(p, isLianti);
        if (playerRealmIdx < pd.realmIndex) {
            String req = isLianti ? getLantiRealmName(pd.realmIndex) : getXufaRealmName(pd.realmIndex);
            p.sendMessage(colorize("&c境界不足，需要 " + req));
            return;
        }

        int xp = (int)(pd.xpAmount * qualityMulti[qualityIdx]);
        boolean success = addXpToPlayer(p, xp, isLianti);
        if (!success) { p.sendMessage("&c给予修为失败"); return; }

        item.setAmount(item.getAmount() - 1);
        p.sendMessage("&a服用 " + qualityNames[qualityIdx] + pd.name + " 获得 " + xp + " 修为");
    }

    private void usePill(Player p, PillData pill, ItemStack item) {
        boolean isLianti = liantiPills.containsValue(pill);
        int playerRealmIdx = getPlayerRealmIndex(p, isLianti);

        if (playerRealmIdx < pill.realmIndex) {
            String requiredRealm = isLianti ? getLantiRealmName(pill.realmIndex) : getXufaRealmName(pill.realmIndex);
            p.sendMessage(colorize(getConfig().getString("realm-too-high-message", "\u5883\u754c\u4e0d\u8db3").replace("{required_realm}", requiredRealm)));
            return;
        }

        boolean success = addXpToPlayer(p, pill.xpAmount, isLianti);
        if (!success) { p.sendMessage("\u00a7c\u7ed9\u4e88\u4fee\u4e3a\u5931\u8d25"); return; }

        item.setAmount(item.getAmount() - 1);
        String msg = getConfig().getString("use-message", "\u670d\u7528 {pill_name} \u83b7\u5f97 {xp_amount} \u4fee\u4e3a");
        p.sendMessage(colorize(msg.replace("{pill_name}", pill.name).replace("{xp_amount}", String.valueOf(pill.xpAmount))));
    }

    private boolean addXpToPlayer(Player p, int xpAmount, boolean isLianti) {
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("XiuXianCore");
            if (plugin == null) return false;
            java.lang.reflect.Method method = plugin.getClass().getMethod("addXp", UUID.class, double.class, boolean.class);
            method.invoke(plugin, p.getUniqueId(), (double) xpAmount, isLianti);
            java.lang.reflect.Method levelUpMethod = plugin.getClass().getMethod("checkLevelUp", UUID.class);
            levelUpMethod.invoke(plugin, p.getUniqueId());
            return true;
        } catch (Exception e) { return false; }
    }

    private int getPlayerRealmIndex(Player p, boolean isLianti) {
        if (xiuxianDataDir == null || !xiuxianDataDir.exists()) return 12;
        File file = new File(xiuxianDataDir, p.getUniqueId().toString() + ".json");
        if (!file.exists()) return 12;
        try {
            JsonObject json = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            int level = isLianti ?
                (json.has("liantiLevel") ? json.get("liantiLevel").getAsInt() : 0) :
                (json.has("xiufaLevel") ? json.get("xiufaLevel").getAsInt() : 0);
            return level / 100;
        } catch (Exception e) { return 12; }
    }

    private PillData findPill(String displayName, Material material) {
        for (PillData pill : liantiPills.values()) {
            if (pill.material == material && pill.name.equals(displayName)) return pill;
        }
        for (PillData pill : xiufaPills.values()) {
            if (pill.material == material && pill.name.equals(displayName)) return pill;
        }
        return null;
    }

    public ItemStack createPillItem(PillData pill) {
        ItemStack item = new ItemStack(pill.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(pill.name);
            List<String> lore = new ArrayList<>();
            lore.add(colorize(pill.description));
            lore.add(colorize("\u00a77\u5883\u754c: " + pill.realm));
            lore.add(colorize("\u00a77\u4fee\u4e3a: +" + pill.xpAmount));
            lore.add("");
            lore.add(colorize("\u00a77\u53f3\u952e\u670d\u7528"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getLantiRealmName(int idx) {
        String[] realms = {"\u901a\u8109", "\u953b\u9aa8", "\u7ec3\u817f", "\u5143\u6b66", "\u795e\u529b", "\u7834\u865a", "\u6df7\u5143", "\u5927\u6210", "\u6d85\u69c3", "\u771f\u6b66", "\u91d1\u76f8", "\u592a\u4e0a", "\u7f57\u5929"};
        return idx >= 0 && idx < realms.length ? realms[idx] : "\u672a\u77e5";
    }

    private String getXufaRealmName(int idx) {
        String[] realms = {"\u7ec3\u6c14", "\u7b51\u57fa", "\u7ed3\u4e39", "\u5143\u5a75", "\u5316\u795e", "\u8fd4\u865a", "\u5408\u4f53", "\u5927\u4e58", "\u6e21\u52ab", "\u771f\u4ed9", "\u91d1\u4ed9", "\u592a\u4e59", "\u5927\u7f57"};
        return idx >= 0 && idx < realms.length ? realms[idx] : "\u672a\u77e5";
    }

    private String colorize(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    static class PillData {
        int realmIndex;
        String name, description, realm, alchemistMaterial;
        int xpAmount;
        Material material;

        PillData(int realmIndex, String name, String description, String realm,
                 int xpAmount, Material material, String alchemistMaterial) {
            this.realmIndex = realmIndex;
            this.name = name;
            this.description = description;
            this.realm = realm;
            this.xpAmount = xpAmount;
            this.material = material;
            this.alchemistMaterial = alchemistMaterial;
        }
    }
}